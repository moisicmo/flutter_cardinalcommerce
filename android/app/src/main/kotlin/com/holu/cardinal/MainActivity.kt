package com.holu.cardinal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalRenderType
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.shared.models.enums.ButtonType
import com.cardinalcommerce.shared.userinterfaces.*
import org.json.JSONArray
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.holu.cardinal/cardinal"
    private val cardinal: Cardinal = Cardinal.getInstance()

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "configureCardinal") {
                configureCardinal()
                result.success("Cardinal Configured")
            } else {
                result.notImplemented()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureCardinal()
    }

    private fun configureCardinal() {
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
    }
}
