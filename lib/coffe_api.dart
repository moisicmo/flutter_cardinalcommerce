import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:dio/io.dart';

import 'logs.dart';

class CoffeApi {
  static final _dio = Dio();
  static Future<void> configureDio(String baseUrl) async {
    // Configurar Headers
    _dio.options.headers = {
      'Content-Type': 'application/json',
    };
    _dio.httpClientAdapter = IOHttpClientAdapter(
      createHttpClient: () {
        final HttpClient client = HttpClient(context: SecurityContext(withTrustedRoots: true));
        client.badCertificateCallback = (cert, host, port) => true;
        return client;
      },
      validateCertificate: (cert, host, port) {
        return true;
      },
    );
    _dio.options.baseUrl = baseUrl;
    _dio.options.connectTimeout = const Duration(seconds: 20);
    _dio.options.receiveTimeout = const Duration(seconds: 40);
  }

  static Future<void> verifyInternet() async {
    try {
      final result = await InternetAddress.lookup('multired.com.bo');
      if (result.isNotEmpty && result[0].rawAddress.isNotEmpty) return;
    } catch (e) {
      throw ('Revise su conexión a internet');
    }
  }

  static Future<Response> get(String path) async {
    try {
      prettyPrint('================= GET =========================================', type: DebugType.statusCode);
      prettyPrint('==PATH== ${_dio.options.baseUrl}$path');
      await verifyInternet();
      final resp = await _dio.get(path);
      prettyPrint('==STATUS== ${resp.statusCode}');
      prettyPrint('==RESPONSE== ${jsonEncode(resp.data)}');
      prettyPrint('===============================================================', type: DebugType.statusCode);
      return resp;
    } on DioException catch (e) {
      prettyPrint('==ERROR== $e', type: DebugType.error);
      prettyPrint('==ERROR== ${e.type}', type: DebugType.error);
      switch (e.type) {
        case DioExceptionType.connectionTimeout:
        case DioExceptionType.unknown:
        return get(path);
        case DioExceptionType.connectionError:
          throw (e.type);
        case DioExceptionType.receiveTimeout:
          throw ('Tenemos un problema de comunicación, intente de nuevo.');
        default:
      }
      rethrow;
    }
  }

  static Future<Response> post(String path, Object data) async {
    try {
      prettyPrint('================= POST =========================================', type: DebugType.statusCode);
      prettyPrint('==PATH== ${_dio.options.baseUrl}$path');
      prettyPrint('==REQUEST== $data');
      await verifyInternet();
      final resp = await _dio.post(path, data: data);
      prettyPrint('==STATUS== ${resp.statusCode}');
      prettyPrint('==RESPONSE== ${jsonEncode(resp.data)}');
      prettyPrint('================================================================', type: DebugType.statusCode);
      return resp;
    } on DioException catch (e) {
      prettyPrint('==ERROR== $e', type: DebugType.error);
      prettyPrint('==ERROR== ${e.type}', type: DebugType.error);
      switch (e.type) {
        case DioExceptionType.connectionTimeout:
        case DioExceptionType.unknown:
          return post(path, data);
        case DioExceptionType.connectionError:
          throw (e.type);
        case DioExceptionType.receiveTimeout:
          throw ('Tenemos un problema de comunicación, intente de nuevo.');
        default:
      }
      rethrow;
    }
  }

}
