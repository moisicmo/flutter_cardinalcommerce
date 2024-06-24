package com.holu.cardinal

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalRenderType
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.shared.models.enums.ButtonType
import com.cardinalcommerce.shared.userinterfaces.*
import org.json.JSONArray
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

import kotlinx.coroutines.*
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : FlutterActivity() {
  private val CHANNEL = "com.holu.cardinal/cardinal"

  private lateinit var cardinal: Cardinal

  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)

    MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
      if (call.method == "configureCardinal") {
        val serverJwt = call.argument<String>("serverJwt")
        if (serverJwt != null) {
          CoroutineScope(Dispatchers.Main).launch {
            try {
              val consumerSessionId = configureCardinal(serverJwt)
              result.success(consumerSessionId)
            } catch (e: Exception) {
              result.error("CONFIGURATION_ERROR", "Failed to configure Cardinal", e.message)
            }
          }
        } else {
          result.error("INVALID_ARGUMENT", "serverJwt is required", null)
        }
      } else {
        result.notImplemented()
      }
    }
  }

  private suspend fun configureCardinal(serverJwt: String): String = suspendCoroutine {
    continuation ->
    cardinal = Cardinal.getInstance()

    val cardinalConfigurationParameters = CardinalConfigurationParameters()
    cardinalConfigurationParameters.environment = CardinalEnvironment.STAGING
    cardinalConfigurationParameters.requestTimeout = 8000
    cardinalConfigurationParameters.challengeTimeout = 5

    val rTYPE = JSONArray()
    rTYPE.put(CardinalRenderType.OTP)
    rTYPE.put(CardinalRenderType.SINGLE_SELECT)
    rTYPE.put(CardinalRenderType.MULTI_SELECT)
    rTYPE.put(CardinalRenderType.OOB)
    rTYPE.put(CardinalRenderType.HTML)
    cardinalConfigurationParameters.renderType = rTYPE

    cardinalConfigurationParameters.uiType = CardinalUiType.BOTH
    cardinalConfigurationParameters.isLocationDataConsentGiven = true

    val yourUICustomizationObject = UiCustomization()
    cardinalConfigurationParameters.uiCustomization = yourUICustomizationObject

    cardinal.configure(this, cardinalConfigurationParameters)

    cardinal.init(serverJwt, object : CardinalInitService {
      override fun onSetupCompleted(consumerSessionId: String) {

        println("Cardinal init setup completed. Consumer session ID: $consumerSessionId")
        continuation.resume(consumerSessionId)
      }

      override fun onValidated(validateResponse: ValidateResponse, serverJwt: String?) {
        if (serverJwt == null) {
          continuation.resumeWithException(Exception("Validation failed: ${validateResponse.errorDescription}"))
        } else {
          println("Cardinal init validated. Server JWT: $serverJwt, validateResponse: $validateResponse")
        }
      }
    })
  }
}
