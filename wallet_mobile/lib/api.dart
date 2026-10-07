import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

class ApiException implements Exception {
  final String message;
  ApiException(this.message);
  @override
  String toString() => message;
}

class WalletApi {
  static String? webWalletUrl;
  static String? webServiceUrl;
  final http.Client client;
  String walletUrl;
  String serviceUrl;
  String? token;
  WalletApi({http.Client? client, String? walletUrl, String? serviceUrl})
    : client = client ?? http.Client(),
      walletUrl = walletUrl ?? (kIsWeb ? webWalletUrl : null) ?? const String.fromEnvironment('WALLET_URL', defaultValue: '') ,
      serviceUrl = serviceUrl ?? (kIsWeb ? webServiceUrl : null) ?? const String.fromEnvironment('SERVICE_URL', defaultValue: '') {
    if (this.walletUrl.isEmpty) {
      this.walletUrl = kIsWeb ? Uri.base.resolve('/wallet').toString() : 'http://10.0.2.2:8091';
    }
    if (this.serviceUrl.isEmpty) {
      this.serviceUrl = kIsWeb ? Uri.base.resolve('/services').toString() : 'http://10.0.2.2:8092';
    }
  }

  Future<dynamic> request(String path, {Map<String, dynamic>? body, bool service = false}) async {
    final base = (service ? serviceUrl : walletUrl).replaceAll(RegExp(r'/+$'), '');
    final headers = {'Content-Type': 'application/json', if (token != null) 'Authorization': 'Bearer $token'};
    try {
      final uri = Uri.parse('$base$path');
      final response = await (body == null
        ? client.get(uri, headers: headers)
        : client.post(uri, headers: headers, body: jsonEncode(body)))
        .timeout(const Duration(seconds: 20));
      dynamic data;
      try { data = response.body.isEmpty ? null : jsonDecode(utf8.decode(response.bodyBytes)); }
      catch (_) { data = null; }
      if (response.statusCode < 200 || response.statusCode >= 300) {
        var message = data is Map ? (data['message'] ?? data.values.join('\n')).toString() : 'Le serveur ne répond pas correctement (${response.statusCode}).';
        if (response.statusCode == 401) message = 'Connexion expirée ou PIN incorrect. Reconnectez-vous.';
        // The prestation service can wrap a wallet error as JSON in its message.
        try { final nested = jsonDecode(message); if (nested is Map && nested['message'] != null) message = nested['message'].toString(); } catch (_) {}
        throw ApiException(message);
      }
      return data;
    } on TimeoutException {
      throw ApiException('Le serveur met trop de temps à répondre. Vérifiez la connexion.');
    } on http.ClientException {
      throw ApiException('Serveur inaccessible. Vérifiez les adresses dans les paramètres et le démarrage du backend.');
    }
  }
}
