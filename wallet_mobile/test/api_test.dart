import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:wallet_mobile/api.dart';
void main() {
  test('Le paiement transmet le JWT et le PIN au service de prestations', () async {
    final api = WalletApi(walletUrl: 'http://wallet', serviceUrl: 'http://service/', client: MockClient((request) async {
      expect(request.url.toString(), 'http://service/api/prestations/1/payer');
      expect(request.headers['Authorization'], 'Bearer test-token');
      expect(jsonDecode(request.body), {'pin': '1234'});
      return http.Response('{"statut":"EN_ATTENTE_PAIEMENT"}', 200);
    }))..token = 'test-token';
    expect((await api.request('/api/prestations/1/payer', service: true, body: {'pin': '1234'}))['statut'], 'EN_ATTENTE_PAIEMENT');
  });
  test('Un refus du backend devient une erreur lisible', () async {
    final api = WalletApi(walletUrl: 'http://wallet', client: MockClient((_) async => http.Response('{"message":"Solde insuffisant"}', 400)));
    await expectLater(api.request('/api/transactions/retrait', body: {'montant':100}), throwsA(isA<ApiException>().having((e) => e.message, 'message', 'Solde insuffisant')));
  });
}
