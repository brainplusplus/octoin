package fr.acinq.eclair.gui.controllers

import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control.{Button, Label, TextArea, TextField}
import javafx.scene.input.KeyCode.{ENTER, TAB}
import javafx.scene.input.KeyEvent
import javafx.stage.Stage

import fr.acinq.bitcoin.{BinaryData, millisatoshi2btc, millisatoshi2satoshi}
import fr.acinq.bitcoin.Crypto.PublicKey
import fr.acinq.eclair.Setup
import fr.acinq.eclair.gui.Handlers
import fr.acinq.eclair.gui.utils.GUIValidators
import fr.acinq.eclair.payment.PaymentRequest
import grizzled.slf4j.Logging

import scala.util.{Failure, Success, Try}


/**
  * Created by DPA on 23/09/2016.
  */
class SendPaymentController(val handlers: Handlers, val stage: Stage) extends Logging {

  @FXML var paymentRequest: TextArea = _
  @FXML var paymentRequestError: Label = _
  @FXML var nodeIdField: TextField = _
  @FXML var amountField: TextField = _
  @FXML var hashField: TextField = _
  @FXML var description: TextField = _
  @FXML var sendButton: Button = _

  @FXML def initialize(): Unit = {
    // ENTER or TAB events in the paymentRequest textarea insted fire or focus sendButton
    paymentRequest.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler[KeyEvent] {
      def handle(event: KeyEvent) = {
        event.getCode match {
          case ENTER =>
            sendButton.fire
            event.consume
          case TAB =>
            sendButton.requestFocus()
            event.consume
          case _ =>
        }
      }
    })
    paymentRequest.textProperty.addListener(new ChangeListener[String] {
      def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String) = {
        Try(PaymentRequest.read(paymentRequest.getText)) match {
          case Success(pr) =>
            pr.amount.foreach(amount => amountField.setText((millisatoshi2satoshi(amount).amount) + " satoshi (" + (millisatoshi2btc(amount).amount) + " OCC)")) //toOCC
            nodeIdField.setText(pr.nodeId.toString)
            hashField.setText(pr.paymentHash.toString)

            val desc = pr.description.toString

            description.setText(desc.substring(5,desc.size - 1))

          case Failure(f) =>
            GUIValidators.validate(paymentRequestError, "Please use a valid payment request", false)
            amountField.setText("0")
            nodeIdField.setText("N/A")
            hashField.setText("N/A")
        }
      }
    })
  }

  @FXML def handleSend(event: ActionEvent) = {
    Try(PaymentRequest.read(paymentRequest.getText)) match {
      case Success(pr) =>
        Try(handlers.send(pr.nodeId, pr.paymentHash, pr.amount.get.amount, pr.minFinalCltvExpiry)) match {
          case Success(s) => stage.close
          case Failure(f) => GUIValidators.validate(paymentRequestError, s"Invalid Payment Request: ${f.getMessage}", false)
        }
      case Failure(f) => GUIValidators.validate(paymentRequestError, "cannot parse payment request", false)
    }
  }

  @FXML def handleClose(event: ActionEvent) = {
    stage.close
  }
}
