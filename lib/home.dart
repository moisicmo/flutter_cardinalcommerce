import 'dart:developer';

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
        try {
      final String result = await platform.invokeMethod('configureCardinal');
      log(result);
    } on PlatformException catch (e) {
      log("Failed to configure Cardinal: '${e.message}'.");
    }

  }
}
