package com.holu.cardinal

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalRenderType
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver
import com.cardinalcommerce.shared.userinterfaces.UiCustomization
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.holu.cardinal.Validated3DSResponse

class MainActivity : FlutterFragmentActivity() {
  private val CHANNEL = "com.holu.cardinal/cardinal"
  private val cardinal = Cardinal.getInstance()

  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)

    MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
      when (call.method) {
        "configureCardinal" -> {
          val serverJwt = call.argument<String>("serverJwt")
          if (serverJwt != null && serverJwt.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
              try {
                val consumerSessionId = Secure3DS().init3DSecure(this@MainActivity, serverJwt, isTest = true) // Cambia isTest según sea necesario
                result.success(consumerSessionId)
              } catch (e: Exception) {
                result.error("CONFIGURATION_ERROR", e.message, null)
              }
            }
          } else {
            result.error("INVALID_ARGUMENT", "serverJwt is required", null)
          }
        }
        "handleAuthenticationResponse" -> {
          val threeDSVersion = call.argument<String>("threeDSVersion")
          val enrolled = call.argument<String>("enrolled")
          val paResStatus = call.argument<String>("paResStatus")
          val transactionId = call.argument<String>("transactionId")
          val payload = call.argument<String>("payload")

          if (threeDSVersion != null && enrolled != null && paResStatus != null && transactionId != null && payload != null) {
            CoroutineScope(Dispatchers.Main).launch {
              try {
                val authResult = Secure3DS().validate(this@MainActivity, transactionId, payload)
                result.success(authResult.toString())
              } catch (e: Exception) {
                result.error("AUTHENTICATION_ERROR", e.message, null)
              }
            }
          } else {
            result.error("INVALID_ARGUMENT", "Missing required fields", null)
          }
        }
        else -> result.notImplemented()
      }
    }
  }
}

class Secure3DS {
  private val cardinal: Cardinal = Cardinal.getInstance()

  suspend fun init3DSecure(context: Context, jwt: String, isTest: Boolean): String {
    val cardinalConfigurationParameters = CardinalConfigurationParameters()

    cardinalConfigurationParameters.environment = if (isTest) {
      CardinalEnvironment.STAGING
    } else {
      CardinalEnvironment.PRODUCTION
    }

    cardinalConfigurationParameters.requestTimeout = 10000
    cardinalConfigurationParameters.challengeTimeout = 5

    val rTYPE = JSONArray().apply {
      put(CardinalRenderType.OTP)
      put(CardinalRenderType.SINGLE_SELECT)
      put(CardinalRenderType.MULTI_SELECT)
      put(CardinalRenderType.OOB)
      put(CardinalRenderType.HTML)
    }
    cardinalConfigurationParameters.renderType = rTYPE
    cardinalConfigurationParameters.uiType = CardinalUiType.BOTH

    val customizationObject = UiCustomization()
    cardinalConfigurationParameters.uiCustomization = customizationObject
    cardinal.configure(context, cardinalConfigurationParameters)

    return suspendCoroutine { cont ->
      val service = object : CardinalInitService {
        override fun onSetupCompleted(consumerSessionId: String) {
          cont.resume(consumerSessionId)
        }

        override fun onValidated(validateResponse: ValidateResponse, jwt1: String?) {
          cont.resumeWithException(Exception("Validation failed: ${validateResponse.errorDescription}"))
        }
      }

      cardinal.init(jwt, service)
    }
  }

    suspend fun validate(activity: Activity, transactionId: String, payload: String): Validated3DSResponse {
      return suspendCoroutine<Validated3DSResponse> { continuation ->
        val onValidateFinish = object : CardinalValidateReceiver {
          override fun onValidated(currentContext: Context?, validateResponse: ValidateResponse, serverJWT: String?) {
            val result = when (validateResponse.actionCode) {
              CardinalActionCode.SUCCESS, CardinalActionCode.NOACTION -> "SUCCESS"
              CardinalActionCode.ERROR -> "ERROR"
              CardinalActionCode.FAILURE -> "FAILURE"
              CardinalActionCode.CANCEL -> "CANCEL"
              CardinalActionCode.TIMEOUT -> "TIMEOUT"
              else -> "UNKNOWN"
            }
            continuation.resume(Validated3DSResponse(false, result))
          }
        }

        try {
          cardinal.cca_continue(transactionId, payload, activity, onValidateFinish)
        } catch (e: Exception) {
          println("error ${e}");
          continuation.resume(Validated3DSResponse(false, "ERROR"))
        }
      }
    }
}
