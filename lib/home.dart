import 'dart:convert';
import 'dart:developer';

import 'package:cardinal/coffe_api.dart';
import 'package:cardinal/logs.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  static const platform = MethodChannel('com.holu.cardinal/cardinal');

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 30),
          child: ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xff3b5998),
            ),
            onPressed: () => payment(),
            child: const Text(
              'PAGAR',
              style: TextStyle(color: Colors.white),
            ),
          ),
        ),
      ),
    );
  }

  payment() async {
    const serverJwt =
        "..";
    try {
      //solicitando referenceId a cardinal
      final String referenceId = await platform.invokeMethod('configureCardinal', {'serverJwt': serverJwt});
      log("Consumer Session ID: $referenceId");
      await sedData(referenceId);
    } on PlatformException catch (e) {
      log("Failed to Cardinal: '${e.message}'.");
    }
  }

  //enviando al backend el DFReferenceId
  sedData(String referenceId) async {
    await CoffeApi.configureDio('http://1:8080/api/');
    String path = 'process-payment';
    Object data = {'DFReferenceId': referenceId};
    final res = await CoffeApi.post(path, data);
    if (res.data['CardinalMPI']['ErrorNo'] != '0') return;

    await prettyPrint('==ThreeDSVersion==${res.data['CardinalMPI']['ThreeDSVersion']}');
    await prettyPrint('==Enrolled==${res.data['CardinalMPI']['Enrolled']}');
    await prettyPrint('==PAResStatus==${res.data['CardinalMPI']['PAResStatus']}');
    await prettyPrint('==TransactionId==${res.data['CardinalMPI']['TransactionId']}');
    await prettyPrint('==Payload==${res.data['CardinalMPI']['Payload']}');
    // Verificar que enrolled sea "Y" y que threeDSVersion empiece con "2"
    // if (enrolled != "Y" || !threeDSVersion.startsWith("2")) {
    //   println("Authentication response does not meet requirements. Canceling operation.")
    //   return
    // }
    final result = await platform.invokeMethod('handleAuthenticationResponse', {
      'threeDSVersion': res.data['CardinalMPI']['ThreeDSVersion'],
      'enrolled': res.data['CardinalMPI']['Enrolled'],
      'paResStatus': res.data['CardinalMPI']['PAResStatus'] is String ? res.data['CardinalMPI']['PAResStatus'] : "",
      'transactionId': res.data['CardinalMPI']['TransactionId'],
      'payload': json.encode(res.data['CardinalMPI']['Payload']),
    });
    log("result: ${result.toString()}");
  }
}
