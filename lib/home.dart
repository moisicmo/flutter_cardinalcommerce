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
        "";
    try {
      final String consumerSessionId = await platform.invokeMethod('configureCardinal', {'serverJwt': serverJwt});
      log("Consumer Session ID: $consumerSessionId");
      await sedData(consumerSessionId);
    } on PlatformException catch (e) {
      log("Failed to configure Cardinal: '${e.message}'.");
    }
  }

  //enviando al backend el DFReferenceId
  sedData(String referenceId) async {
    await CoffeApi.configureDio('http://localhost:8080/api/');
    String path = 'process-payment';
    Object data = {
      'DFReferenceId': referenceId,
    };
    final res = await CoffeApi.post(path, data);
    if (res.data['CardinalMPI']['ErrorNo'] != '0') return;

    await prettyPrint('==ThreeDSVersion==${res.data['CardinalMPI']['ThreeDSVersion']}');
    await prettyPrint('==Enrolled==${res.data['CardinalMPI']['Enrolled']}');
    await prettyPrint('==PAResStatus==${res.data['CardinalMPI']['PAResStatus']}');
    await prettyPrint('==TransactionId==${res.data['CardinalMPI']['TransactionId']}');
    await prettyPrint('==Payload==${res.data['CardinalMPI']['Payload']}');

    await platform.invokeMethod('handleAuthenticationResponse', {
      'threeDSVersion': res.data['CardinalMPI']['ThreeDSVersion'],
      'enrolled': res.data['CardinalMPI']['Enrolled'],
      'paResStatus': res.data['CardinalMPI']['PAResStatus'] is String ? res.data['CardinalMPI']['PAResStatus'] : "",
      'transactionId': res.data['CardinalMPI']['TransactionId'],
      'payload': json.encode(res.data['CardinalMPI']['Payload']),
    });
  }
}
