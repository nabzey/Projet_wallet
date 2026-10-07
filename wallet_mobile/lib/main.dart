import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'api.dart';

const ink = Color(0xFF0B1226);
const blue = Color(0xFF2056F3);
const accent = Color(0xFF38D9F7);
String money(dynamic n) => '${(n ?? 0).toString().replaceAllMapped(RegExp(r'(\d)(?=(\d{3})+(?!\d))'), (m) => '${m[1]} ')} FCFA';
String status(dynamic value) => switch (value) {
  'PAYEE' => 'Payée', 'EN_ATTENTE_PAIEMENT' => 'À payer',
  'ECHEC_PAIEMENT' => 'Paiement échoué', 'EN_COURS' => 'En cours',
  'TERMINEE' => 'Terminée', 'A_FAIRE' => 'À faire', _ => '$value',
};
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  if (kIsWeb) {
    try {
      final response = await http.get(Uri.base.resolve('/config.json')).timeout(const Duration(seconds: 3));
      if (response.statusCode == 200) {
        final config = jsonDecode(response.body) as Map<String, dynamic>;
        WalletApi.webWalletUrl = config['walletUrl'] as String?;
        WalletApi.webServiceUrl = config['serviceUrl'] as String?;
      }
    } catch (_) { /* Local demo uses its same-origin proxy. */ }
  }
  runApp(const WalletApp());
}
class WalletApp extends StatelessWidget {
  const WalletApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'Sama Wallet', debugShowCheckedModeBanner: false,
    theme: ThemeData(useMaterial3: true, scaffoldBackgroundColor: const Color(0xFFF3F6FC),
      colorScheme: ColorScheme.fromSeed(seedColor: blue, primary: blue, secondary: accent),
      appBarTheme: const AppBarTheme(backgroundColor: Color(0xFFF3F6FC), foregroundColor: ink, centerTitle: false),
      inputDecorationTheme: InputDecorationTheme(filled: true, fillColor: Colors.white,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: Color(0xFFDCE4F5))),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: Color(0xFFDCE4F5)))),
      filledButtonTheme: FilledButtonThemeData(style: FilledButton.styleFrom(minimumSize: const Size(0, 52), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)))),
      cardTheme: CardThemeData(elevation: 0, color: Colors.white, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20))),
    ), home: const WalletHome());
}

class WalletHome extends StatefulWidget {
  const WalletHome({super.key});
  @override
  State<WalletHome> createState() => _WalletHomeState();
}
class _WalletHomeState extends State<WalletHome> {
  final api = WalletApi();
  final phone = TextEditingController();
  final pin = TextEditingController();
  final otp = TextEditingController();
  bool busy = false, register = false;
  int step = 0, tab = 0;
  String? error, verificationToken;
  Map<String, dynamic>? account;
  List<dynamic> transactions = [], prestations = [];
  Timer? timer;
  @override
  void dispose() { timer?.cancel(); phone.dispose(); pin.dispose(); otp.dispose(); api.client.close(); super.dispose(); }
  void message(String text) { if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text))); }
  Future<void> action(Future<void> Function() work) async {
    if (busy) return;
    setState(() { busy = true; error = null; });
    try { await work(); } catch (e) { if (mounted) { setState(() => error = e.toString()); message(e.toString()); } }
    finally { if (mounted) setState(() => busy = false); }
  }
  Future<void> refresh() async {
    final result = await Future.wait([
      api.request('/api/comptes/moi'), api.request('/api/comptes/moi/transactions'),
      api.request('/api/prestations/moi', service: true),
    ]);
    if (mounted) setState(() { account = Map<String,dynamic>.from(result[0]); transactions = result[1]; prestations = result[2]; });
  }
  Future<void> authenticate() => action(() async {
    final telephone = phone.text.trim();
    if (!RegExp(r'^\+?[0-9]{9,15}$').hasMatch(telephone)) throw ApiException('Saisissez un numéro de téléphone valide.');
    if (register && step == 0) {
      await api.request('/api/auth/request-otp', body: {'telephone': telephone});
      setState(() => step = 1); return;
    }
    if (register && step == 1) {
      final data = await api.request('/api/auth/verify-otp', body: {'telephone': telephone, 'code': otp.text.trim()});
      verificationToken = data['verificationToken']; setState(() => step = 2); return;
    }
    if (!RegExp(r'^\d{4}$').hasMatch(pin.text)) throw ApiException('Le PIN doit contenir exactement 4 chiffres.');
    final data = await api.request(register ? '/api/auth/create-pin' : '/api/auth/login', body: {
      'telephone': telephone, 'pin': pin.text, if (register) 'verificationToken': verificationToken,
    });
    api.token = data['token'];
    try { await refresh(); } catch (_) { api.token = null; rethrow; }
    pin.clear();
    timer?.cancel();
    timer = Timer.periodic(const Duration(seconds: 6), (_) async {
      if (api.token == null || busy) return;
      // Kafka confirms payments asynchronously. Refresh pending requests only.
      if (prestations.any((p) => p['statut'] == 'EN_ATTENTE_PAIEMENT')) {
        try { await refresh(); } catch (_) { /* Manual refresh displays connection errors. */ }
      }
    });
  });
  Future<void> settings() async {
    final wallet = TextEditingController(text: api.walletUrl);
    final service = TextEditingController(text: api.serviceUrl);
    final values = await form('Connexion aux serveurs', [
      field(wallet, 'URL du portefeuille', keyboard: TextInputType.url),
      field(service, 'URL des prestations', keyboard: TextInputType.url),
    ], () {
      for (final c in [wallet,service]) {
        final uri = Uri.tryParse(c.text.trim());
        if (uri == null || !['http','https'].contains(uri.scheme) || uri.host.isEmpty) return 'Saisissez des URL http ou https valides.';
      }
      return null;
    });
    if (values == true) { api.walletUrl = wallet.text.trim(); api.serviceUrl = service.text.trim(); message('Adresses mises à jour.'); }
    // Controllers are kept until the dialog route has completed its closing animation.
    await Future<void>.delayed(const Duration(milliseconds: 300)); wallet.dispose(); service.dispose();
  }
  Widget field(TextEditingController c, String label, {bool secret = false, TextInputType? keyboard}) => Padding(
    padding: const EdgeInsets.only(bottom: 14), child: TextField(controller: c, obscureText: secret,
      keyboardType: keyboard, inputFormatters: secret ? [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(4)] : null,
      decoration: InputDecoration(labelText: label), onSubmitted: (_) {}));
  Future<bool?> form(String title, List<Widget> fields, String? Function() validate) => showDialog<bool>(
    context: context, builder: (context) { String? validation; return StatefulBuilder(builder: (context, update) => AlertDialog(
      title: Text(title), content: SizedBox(width: 380, child: SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, children: [
        ...fields, if (validation != null) Text(validation!, style: const TextStyle(color: Colors.red)),
      ]))), actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
        FilledButton(onPressed: () { final issue = validate(); if (issue != null) { update(() => validation = issue); return; } Navigator.pop(context, true); }, child: const Text('Confirmer'))],
    )); });
  Future<void> transaction(bool deposit) async {
    final amount = TextEditingController(); final code = TextEditingController();
    final ok = await form(deposit ? 'Faire un dépôt' : 'Faire un retrait', [
      field(amount, 'Montant en FCFA', keyboard: TextInputType.number),
      if (!deposit) field(code, 'Votre PIN', secret: true, keyboard: TextInputType.number),
    ], () => (int.tryParse(amount.text) ?? 0) <= 0 ? 'Saisissez un montant entier positif.' : (!deposit && code.text.length != 4 ? 'Saisissez votre PIN à 4 chiffres.' : null));
    if (ok == true) { await action(() async {
      await api.request('/api/transactions/${deposit ? 'depot' : 'retrait'}', body: {'montant': int.parse(amount.text), if (!deposit) 'pin': code.text});
      await refresh(); message(deposit ? 'Dépôt effectué.' : 'Retrait effectué.');
    }); }
    await Future<void>.delayed(const Duration(milliseconds: 300)); amount.dispose(); code.dispose();
  }
  Future<void> createPrestation() async {
    final title = TextEditingController(); final description = TextEditingController(); final amount = TextEditingController();
    final ok = await form('Nouvelle prestation', [field(title, 'Titre'), field(description, 'Description'), field(amount, 'Montant en FCFA', keyboard: TextInputType.number)],
      () => title.text.trim().isEmpty ? 'Ajoutez un titre.' : (int.tryParse(amount.text) ?? 0) <= 0 ? 'Saisissez un montant entier positif.' : null);
    if (ok == true) { await action(() async {
      await api.request('/api/prestations', service: true, body: {'titre': title.text.trim(), 'description': description.text.trim(), 'montant': int.parse(amount.text)});
      await refresh(); setState(() => tab = 1); message('Prestation créée.');
    }); }
    await Future<void>.delayed(const Duration(milliseconds: 300)); title.dispose(); description.dispose(); amount.dispose();
  }
  Future<void> pay(Map<String,dynamic> p) async {
    final code = TextEditingController();
    final ok = await form('Payer ${money(p['montant'])}', [Text(p['titre']), const SizedBox(height: 16), field(code, 'Votre PIN', secret: true, keyboard: TextInputType.number)], () => code.text.length == 4 ? null : 'Saisissez votre PIN à 4 chiffres.');
    if (ok == true) { await action(() async {
      await api.request('/api/prestations/${p['id']}/payer', service: true, body: {'pin': code.text});
      await refresh(); message('Paiement envoyé. La confirmation arrive dans quelques secondes.');
    }); }
    await Future<void>.delayed(const Duration(milliseconds: 300)); code.dispose();
  }
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Row(children: [Container(padding: const EdgeInsets.all(8), decoration: BoxDecoration(gradient: const LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [blue, Color(0xFF4F7CFF)]), borderRadius: BorderRadius.circular(12)), child: const Icon(Icons.account_balance_wallet_rounded, color: accent, size: 22)), const SizedBox(width: 10), const Text('sama wallet', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: -1))]),
      actions: [if (account == null) IconButton(onPressed: busy ? null : settings, tooltip: 'Paramètres', icon: const Icon(Icons.tune_rounded)),
        if (account != null) IconButton(tooltip: 'Se déconnecter', onPressed: busy ? null : () { timer?.cancel(); setState(() { api.token = null; account = null; transactions = []; prestations = []; step = 0; register = false; error = null; }); }, icon: const Icon(Icons.logout_rounded)), const SizedBox(width: 12)]),
    body: Column(children: [if (busy) const LinearProgressIndicator(minHeight: 3), Expanded(child: Center(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 880), child: account == null ? authView() : dashboard())))]),
    bottomNavigationBar: account == null ? null : NavigationBar(selectedIndex: tab, onDestinationSelected: (v) => setState(() => tab = v), destinations: const [
      NavigationDestination(icon: Icon(Icons.grid_view_rounded), label: 'Accueil'),
      NavigationDestination(icon: Icon(Icons.work_outline_rounded), label: 'Prestations'),
      NavigationDestination(icon: Icon(Icons.receipt_long_rounded), label: 'Historique'),
    ]),
  );
  Widget authView() => SingleChildScrollView(padding: const EdgeInsets.all(24), child: Center(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 440), child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
    const SizedBox(height: 20), Align(alignment: Alignment.centerLeft, child: Container(padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7), decoration: BoxDecoration(color: accent, borderRadius: BorderRadius.circular(30)), child: const Text('VOTRE QUOTIDIEN, SIMPLIFIÉ', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 1)))),
    const SizedBox(height: 22), const Text('Votre argent.\nVos projets.', style: TextStyle(fontSize: 44, fontWeight: FontWeight.w800, height: 1.06, letterSpacing: -2, color: ink)),
    const SizedBox(height: 14), const Text('Gérez votre portefeuille et payez vos prestations en quelques gestes.', style: TextStyle(fontSize: 16, color: Color(0xFF64748B), height: 1.5)),
    const SizedBox(height: 30), SegmentedButton<bool>(segments: const [ButtonSegment(value: false, label: Text('Connexion')), ButtonSegment(value: true, label: Text('Créer un compte'))], selected: {register}, onSelectionChanged: busy ? null : (v) => setState(() { register = v.first; step = 0; error = null; pin.clear(); otp.clear(); })),
    const SizedBox(height: 24), if (register) Padding(padding: const EdgeInsets.only(bottom: 14), child: Text('ÉTAPE ${step + 1} / 3 · ${['Votre numéro', 'Vérification', 'Votre code PIN'][step]}', style: const TextStyle(color: blue, fontWeight: FontWeight.w700))),
    if (step == 0 || !register) field(phone, 'Numéro de téléphone', keyboard: TextInputType.phone),
    if (register && step == 1) ...[const Text('Le SMS est simulé pour cet exercice. Récupérez le code dans les logs du portefeuille.', style: TextStyle(height: 1.5)), const SizedBox(height: 12), field(otp, 'Code OTP reçu', keyboard: TextInputType.number)],
    if (!register || step == 2) field(pin, register ? 'Choisir un PIN à 4 chiffres' : 'Votre PIN à 4 chiffres', secret: true, keyboard: TextInputType.number),
    if (error != null) Padding(padding: const EdgeInsets.only(bottom: 14), child: Text(error!, style: const TextStyle(color: Colors.red))),
    FilledButton.icon(onPressed: busy ? null : authenticate, icon: const Icon(Icons.arrow_forward_rounded), label: Text(busy ? 'Un instant…' : register ? ['Recevoir mon code', 'Vérifier le code', 'Ouvrir mon portefeuille'][step] : 'Se connecter')),
    const SizedBox(height: 22), const Text('PROJET ACADÉMIQUE · FCFA', textAlign: TextAlign.center, style: TextStyle(fontSize: 11, letterSpacing: 1.5, color: Color(0xFF64748B))),
  ]))));
  Widget dashboard() => RefreshIndicator(onRefresh: () => action(refresh), child: ListView(padding: const EdgeInsets.all(22), children: [
    Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(['Votre espace', 'Vos projets', 'Votre activité'][tab], style: const TextStyle(color: Color(0xFF64748B))), Text(['Bonjour 👋', 'Prestations', 'Historique'][tab], style: const TextStyle(fontSize: 30, fontWeight: FontWeight.w800, color: ink, letterSpacing: -1))])), IconButton(onPressed: busy ? null : () => action(refresh), tooltip: 'Actualiser', icon: const Icon(Icons.refresh_rounded))]),
    const SizedBox(height: 22), if (error != null) Padding(padding: const EdgeInsets.only(bottom: 12), child: Text(error!, style: const TextStyle(color: Colors.red))),
    if (tab == 0) ...[
      Container(padding: const EdgeInsets.all(26), decoration: BoxDecoration(gradient: const LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [ink, Color(0xFF14245C)]), borderRadius: BorderRadius.circular(26), boxShadow: [BoxShadow(color: blue.withValues(alpha: 0.25), blurRadius: 24, offset: const Offset(0, 12))]), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [Text('SOLDE DISPONIBLE', style: TextStyle(color: Color(0xFFA9BEE0), fontSize: 12, letterSpacing: 1.5)), Icon(Icons.account_balance_wallet_outlined, color: accent)]),
        const SizedBox(height: 22), FittedBox(fit: BoxFit.scaleDown, child: Text(money(account!['solde']), style: const TextStyle(color: Colors.white, fontSize: 38, fontWeight: FontWeight.w700, letterSpacing: -1))),
        const SizedBox(height: 22), Text(account!['numero'], style: const TextStyle(color: Color(0xFFA9BEE0), fontSize: 12)),
        const SizedBox(height: 22), Row(children: [Expanded(child: FilledButton.icon(style: FilledButton.styleFrom(backgroundColor: accent, foregroundColor: ink), onPressed: busy ? null : () => transaction(true), icon: const Icon(Icons.add_rounded), label: const Text('Déposer'))), const SizedBox(width: 12), Expanded(child: OutlinedButton.icon(style: OutlinedButton.styleFrom(foregroundColor: Colors.white, minimumSize: const Size(0,52), side: const BorderSide(color: Color(0xFF3A5A8C))), onPressed: busy ? null : () => transaction(false), icon: const Icon(Icons.north_east_rounded), label: const Text('Retirer')))]),
      ])), const SizedBox(height: 22),
      Card(child: InkWell(borderRadius: BorderRadius.circular(20), onTap: busy ? null : createPrestation, child: const Padding(padding: EdgeInsets.all(20), child: Row(children: [CircleAvatar(backgroundColor: accent, foregroundColor: ink, child: Icon(Icons.add_task_rounded)), SizedBox(width: 14), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Un nouveau projet ?', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 17)), SizedBox(height: 4), Text('Créez et suivez votre prestation.', style: TextStyle(color: Color(0xFF64748B)))])), Icon(Icons.arrow_forward_rounded)])))),
      const SizedBox(height: 22), heading('Dernières opérations', () => setState(() => tab = 2)),
      if (transactions.isEmpty) empty('Votre portefeuille est prêt', 'Commencez par effectuer un dépôt.', Icons.savings_outlined) else ...transactions.take(4).map(transactionTile),
    ],
    if (tab == 1) ...[FilledButton.icon(onPressed: busy ? null : createPrestation, icon: const Icon(Icons.add), label: const Text('Nouvelle prestation')), const SizedBox(height: 20),
      if (prestations.isEmpty) empty('Place à vos projets', 'Ajoutez une prestation pour commencer.', Icons.work_outline) else ...prestations.reversed.map((p) => prestationTile(Map<String,dynamic>.from(p)))],
    if (tab == 2) ...[Text('${transactions.length} opération(s)', style: const TextStyle(color: Color(0xFF64748B))), const SizedBox(height: 14), if (transactions.isEmpty) empty('Aucune opération', 'Vos dépôts, retraits et paiements apparaîtront ici.', Icons.receipt_long_outlined) else ...transactions.map(transactionTile)],
  ]));
  Widget heading(String text, VoidCallback more) => Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [Text(text, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700)), TextButton(onPressed: more, child: const Text('Tout voir'))]);
  Widget empty(String title, String description, IconData icon) => Padding(padding: const EdgeInsets.symmetric(vertical: 32), child: Column(children: [Icon(icon, size: 42, color: blue), const SizedBox(height: 12), Text(title, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 18)), const SizedBox(height: 8), Text(description, textAlign: TextAlign.center, style: const TextStyle(color: Color(0xFF64748B)))]));
  Widget transactionTile(dynamic t) {
    final deposit = t['type'] == 'DEPOT';
    final date = DateTime.tryParse(t['dateTransaction'] ?? '');
    final label = switch (t['type']) {'DEPOT' => 'Dépôt', 'RETRAIT' => 'Retrait', _ => 'Paiement'};
    return Card(child: Padding(padding: const EdgeInsets.all(16), child: Row(children: [CircleAvatar(backgroundColor: deposit ? const Color(0xFFE3EEFF) : const Color(0xFFE9EDF5), foregroundColor: ink, child: Icon(deposit ? Icons.south_west : Icons.north_east)), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(label, style: const TextStyle(fontWeight: FontWeight.w700)), const SizedBox(height: 4), Text(date == null ? '' : '${date.day.toString().padLeft(2,'0')}/${date.month.toString().padLeft(2,'0')} · ${date.hour.toString().padLeft(2,'0')}:${date.minute.toString().padLeft(2,'0')}', style: const TextStyle(color: Color(0xFF64748B), fontSize: 12))])), Flexible(child: Text('${deposit ? '+' : '−'} ${money(t['montant'])}', textAlign: TextAlign.right, style: TextStyle(fontWeight: FontWeight.w700, color: deposit ? blue : ink)))])));
  }
  Widget prestationTile(Map<String,dynamic> p) => Card(margin: const EdgeInsets.only(bottom: 12), child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Row(children: [Expanded(child: Text(p['titre'], style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700))), const SizedBox(width: 8), Chip(label: Text(status(p['statut']), style: const TextStyle(fontSize: 11)), backgroundColor: p['statut'] == 'PAYEE' ? const Color(0xFFE3EEFF) : const Color(0xFFFFEFCB), side: BorderSide.none)]),
    if ((p['description'] ?? '').isNotEmpty) Padding(padding: const EdgeInsets.symmetric(vertical: 8), child: Text(p['description'], style: const TextStyle(color: Color(0xFF64748B)))),
    Text(money(p['montant']), style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: ink)), const SizedBox(height: 14),
    Wrap(spacing: 8, runSpacing: 8, children: [if (['EN_ATTENTE_PAIEMENT','ECHEC_PAIEMENT'].contains(p['statut'])) FilledButton.icon(onPressed: busy ? null : () => pay(p), icon: const Icon(Icons.lock_outline, size: 18), label: const Text('Payer')), OutlinedButton(onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ProjectPage(api: api, project: p))), child: const Text('Ressources et tâches'))]),
  ])));
}

class ProjectPage extends StatefulWidget {
  final WalletApi api;
  final Map<String,dynamic> project;
  const ProjectPage({super.key, required this.api, required this.project});
  @override
  State<ProjectPage> createState() => _ProjectPageState();
}
class _ProjectPageState extends State<ProjectPage> {
  List<dynamic> resources = [];
  Map<int,List<dynamic>> tasks = {};
  bool loading = true;
  String? error;
  @override
  void initState() { super.initState(); load(); }
  Future<void> load() async {
    try {
      final list = await widget.api.request('/api/prestations/${widget.project['id']}/ressources', service: true) as List;
      final entries = await Future.wait(list.map((r) async => MapEntry(r['id'] as int, await widget.api.request('/api/ressources/${r['id']}/taches', service: true) as List)));
      if (mounted) setState(() { resources = list; tasks = Map.fromEntries(entries); error = null; });
    } catch (e) { if (mounted) setState(() => error = e.toString()); }
    finally { if (mounted) setState(() => loading = false); }
  }
  Future<void> add({int? resourceId}) async {
    final first = TextEditingController(); final second = TextEditingController();
    DateTime deadline = DateTime.now().add(const Duration(days: 7));
    final ok = await showDialog<bool>(context: context, builder: (context) => StatefulBuilder(builder: (context, update) => AlertDialog(
      title: Text(resourceId == null ? 'Affecter une ressource' : 'Nouvelle tâche'),
      content: SizedBox(width: 380, child: Column(mainAxisSize: MainAxisSize.min, children: [
        TextField(controller: first, decoration: InputDecoration(labelText: resourceId == null ? 'Nom de la personne' : 'Libellé de la tâche')), const SizedBox(height: 16),
        if (resourceId == null) TextField(controller: second, decoration: const InputDecoration(labelText: 'Spécialité')) else TextButton.icon(onPressed: () async {
          final chosen = await showDatePicker(context: context, initialDate: deadline, firstDate: DateTime.now().add(const Duration(days: 1)), lastDate: DateTime.now().add(const Duration(days: 3650)));
          if (chosen != null) update(() => deadline = chosen);
        }, icon: const Icon(Icons.calendar_today_outlined), label: Text('Échéance : ${deadline.day}/${deadline.month}/${deadline.year}')),
      ])), actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')), FilledButton(onPressed: () { if (first.text.trim().isNotEmpty && (resourceId != null || second.text.trim().isNotEmpty)) Navigator.pop(context, true); }, child: const Text('Ajouter'))],
    )));
    if (ok == true && mounted) {
      setState(() { loading = true; error = null; });
      try {
        await widget.api.request(resourceId == null ? '/api/prestations/${widget.project['id']}/ressources' : '/api/ressources/$resourceId/taches', service: true,
          body: resourceId == null ? {'nom': first.text.trim(), 'specialite': second.text.trim()} : {'libelle': first.text.trim(), 'delaiRealisation': '${deadline.year}-${deadline.month.toString().padLeft(2,'0')}-${deadline.day.toString().padLeft(2,'0')}'});
        await load();
      } catch (e) { if (mounted) setState(() { error = e.toString(); loading = false; }); }
    }
    await Future<void>.delayed(const Duration(milliseconds: 300)); first.dispose(); second.dispose();
  }
  @override
  Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: Text(widget.project['titre'])), body: Center(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 880), child: ListView(padding: const EdgeInsets.all(22), children: [
    const Text('L’équipe du projet', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800, color: ink)), const SizedBox(height: 8), const Text('Affectez des personnes et organisez leur travail.'), const SizedBox(height: 22),
    FilledButton.icon(onPressed: loading ? null : () => add(), icon: const Icon(Icons.person_add_alt), label: const Text('Ajouter une ressource')), const SizedBox(height: 20),
    if (loading) const LinearProgressIndicator(), if (error != null) ...[Text(error!, style: const TextStyle(color: Colors.red)), TextButton(onPressed: load, child: const Text('Réessayer'))],
    if (!loading && resources.isEmpty) const Padding(padding: EdgeInsets.all(30), child: Text('Aucune ressource affectée pour le moment.', textAlign: TextAlign.center)),
    ...resources.map((r) => Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      ListTile(contentPadding: EdgeInsets.zero, leading: const CircleAvatar(backgroundColor: accent, foregroundColor: ink, child: Icon(Icons.person_outline)), title: Text(r['nom'], style: const TextStyle(fontWeight: FontWeight.w700)), subtitle: Text(r['specialite'])),
      ...?(tasks[r['id']]?.map((t) => ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.radio_button_unchecked, size: 20), title: Text(t['libelle']), subtitle: Text('Échéance : ${t['delaiRealisation']} · ${status(t['statut'])}')))),
      TextButton.icon(onPressed: loading ? null : () => add(resourceId: r['id']), icon: const Icon(Icons.add), label: const Text('Ajouter une tâche')),
    ])))),
  ]))));
}
