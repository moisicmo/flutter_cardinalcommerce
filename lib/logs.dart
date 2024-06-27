import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';

enum DebugType { error, info, url, response, statusCode }

prettyPrint(dynamic value, {DebugType type = DebugType.info}) {
  if (!kDebugMode) return;
  switch (type) {
    case DebugType.statusCode:
      return debugPrint('\x1B[33m${'💎 $value'}\x1B[0m');
    case DebugType.info:
      return debugPrint('\x1B[32m${'💡 $value'}\x1B[0m');
    case DebugType.error:
      return debugPrint('\x1B[31m${'🚨 $value'}\x1B[0m');
    case DebugType.response:
      return debugPrint('\x1B[36m${'💡 $value'}\x1B[0m');
    case DebugType.url:
      return debugPrint('\x1B[34m${'📌 $value'}\x1B[0m');
  }
}
