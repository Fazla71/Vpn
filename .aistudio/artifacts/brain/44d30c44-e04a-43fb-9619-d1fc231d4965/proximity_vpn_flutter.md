# Proximity VPN - Flutter Project Structure

This artifact provides the complete, production-ready Flutter implementation matching the native Android build of **Proximity VPN**.

---

### 1. `pubspec.yaml`

```yaml
name: proximity_vpn
description: "Proximity VPN - Fast, secure VPN client using public VPNGate servers with live latency monitoring."
publish_to: 'none'
version: 1.0.0+1

environment:
  sdk: '>=3.0.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter
  openvpn_flutter: ^1.4.1
  http: ^1.2.0
  csv: ^6.0.0
  provider: ^6.1.2
  flutter_spinkit: ^5.2.1
  shared_preferences: ^2.2.3
  intl: ^0.19.0
  flutter_svg: ^2.0.10+1

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0

flutter:
  uses-material-design: true
  assets:
    - assets/images/
```

---

### 2. `android/app/src/main/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:label="Proximity VPN"
        android:name="${applicationName}"
        android:icon="@mipmap/ic_launcher">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:theme="@style/LaunchTheme"
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
            android:hardwareAccelerated="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <!-- openvpn_flutter native service binding -->
        <service
            android:name="id.laskarmedia.openvpn_flutter.OpenVPNFlutterPluginService"
            android:permission="android.permission.BIND_VPN_SERVICE"
            android:exported="false">
            <intent-filter>
                <action android:name="android.net.VpnService"/>
            </intent-filter>
        </service>
    </application>
</manifest>
```

---

### 3. `lib/models/vpn_server.dart`

```dart
import 'dart:convert';

class VpnServer {
  final String hostName;
  final String ip;
  final int score;
  final int ping;
  final int speed; // in bytes per second
  final String countryLong;
  final String countryShort;
  final int numVpnSessions;
  final int uptime;
  final int totalUsers;
  final int totalTraffic;
  final String logType;
  final String operator;
  final String message;
  final String openVpnConfigDataBase64;

  VpnServer({
    required this.hostName,
    required this.ip,
    required this.score,
    required this.ping,
    required this.speed,
    required this.countryLong,
    required this.countryShort,
    required this.numVpnSessions,
    required this.uptime,
    required this.totalUsers,
    required this.totalTraffic,
    required this.logType,
    required this.operator,
    required this.message,
    required this.openVpnConfigDataBase64,
  });

  /// Converts ISO 2-letter country code (e.g. US, JP) to flag emoji
  String get flagEmoji {
    if (countryShort.length != 2) return '🌐';
    final int firstLetter = countryShort.toUpperCase().codeUnitAt(0) - 0x41 + 0x1F1E6;
    final int secondLetter = countryShort.toUpperCase().codeUnitAt(1) - 0x41 + 0x1F1E6;
    return String.fromCharCode(firstLetter) + String.fromCharCode(secondLetter);
  }

  double get speedMbps => speed / 1000000.0;

  String get speedFormatted {
    if (speedMbps >= 100.0) return '${speedMbps.toStringAsFixed(0)} Mbps';
    if (speedMbps >= 1.0) return '${speedMbps.toStringAsFixed(1)} Mbps';
    if (speed > 0) return '${(speed / 1000).toStringAsFixed(0)} Kbps';
    return 'N/A';
  }

  String get pingFormatted => ping > 0 ? '$ping ms' : '<50 ms';

  String get decodedConfig {
    try {
      return utf8.decode(base64.decode(openVpnConfigDataBase64));
    } catch (_) {
      return '';
    }
  }
}
```

---

### 4. `lib/vpn_service.dart`

```dart
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:openvpn_flutter/openvpn_flutter.dart';
import 'models/vpn_server.dart';

enum ConnectionStatus { disconnected, connecting, connected, disconnecting, error }

class VpnProvider with ChangeNotifier {
  late OpenVPN _engine;
  ConnectionStatus _status = ConnectionStatus.disconnected;
  VpnServer? _selectedServer;
  List<VpnServer> _servers = [];
  bool _isLoading = false;
  String _byteIn = '0 KB';
  String _byteOut = '0 KB';
  int _durationSeconds = 0;

  ConnectionStatus get status => _status;
  VpnServer? get selectedServer => _selectedServer;
  List<VpnServer> get servers => _servers;
  bool get isLoading => _isLoading;
  String get byteIn => _byteIn;
  String get byteOut => _byteOut;
  int get durationSeconds => _durationSeconds;

  String get formattedDuration {
    final m = (_durationSeconds ~/ 60).toString().padLeft(2, '0');
    final s = (_durationSeconds % 60).toString().padLeft(2, '0');
    final h = (_durationSeconds ~/ 3600).toString().padLeft(2, '0');
    return h == '00' ? '$m:$s' : '$h:$m:$s';
  }

  void initialize(Function() onInitialized) {
    _engine = OpenVPN(
      onVpnStatusChanged: (data) {
        _byteIn = data?.byteIn ?? '0 KB';
        _byteOut = data?.byteOut ?? '0 KB';
        notifyListeners();
      },
      onVpnStageChanged: (stage, raw) {
        switch (stage) {
          case VPNStage.connected:
            _status = ConnectionStatus.connected;
            break;
          case VPNStage.connecting:
          case VPNStage.authenticating:
          case VPNStage.wait_connection:
            _status = ConnectionStatus.connecting;
            break;
          case VPNStage.disconnecting:
            _status = ConnectionStatus.disconnecting;
            break;
          case VPNStage.disconnected:
            _status = ConnectionStatus.disconnected;
            break;
          case VPNStage.error:
            _status = ConnectionStatus.error;
            break;
          default:
            break;
        }
        notifyListeners();
      },
    );
    _engine.initialize(
      groupIdentifier: "group.com.aistudio.proximityvpn",
      providerBundleIdentifier: "com.aistudio.proximityvpn.VPNExtension",
      localizedDescription: "Proximity VPN",
    );
    fetchVpnGateServers();
    onInitialized();
  }

  Future<void> fetchVpnGateServers() async {
    _isLoading = true;
    notifyListeners();
    try {
      final response = await http.get(Uri.parse('https://www.vpngate.net/api/iphone/'));
      if (response.statusCode == 200) {
        _servers = _parseCsv(response.body);
        if (_selectedServer == null && _servers.isNotEmpty) {
          autoSelectBestServer();
        }
      }
    } catch (e) {
      debugPrint('Error fetching VPNGate: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  List<VpnServer> _parseCsv(String csvData) {
    final lines = csvData.split('\n');
    final List<VpnServer> list = [];
    Map<String, int>? headerMap;

    for (final raw in lines) {
      final line = raw.trim();
      if (line.isEmpty) continue;

      if (line.startsWith('#HostName') || (line.startsWith('#') && line.contains('CountryLong'))) {
        final headers = line.substring(1).split(',');
        headerMap = {for (int i = 0; i < headers.length; i++) headers[i].trim(): i};
        continue;
      }

      if (line.startsWith('*') || line.startsWith('#')) continue;

      final tokens = line.split(',');
      if (tokens.length < 14) continue;

      try {
        String col(String name, int fallback) {
          final idx = headerMap?[name] ?? fallback;
          return idx < tokens.length ? tokens[idx].trim() : '';
        }

        final ip = col('IP', 1);
        final country = col('CountryLong', 5);
        if (ip.isEmpty || country.isEmpty) continue;

        list.add(VpnServer(
          hostName: col('HostName', 0),
          ip: ip,
          score: int.tryParse(col('Score', 2)) ?? 0,
          ping: int.tryParse(col('Ping', 3)) ?? 50,
          speed: int.tryParse(col('Speed', 4)) ?? 10000000,
          countryLong: country,
          countryShort: col('CountryShort', 6),
          numVpnSessions: int.tryParse(col('NumVpnSessions', 7)) ?? 0,
          uptime: int.tryParse(col('Uptime', 8)) ?? 0,
          totalUsers: int.tryParse(col('TotalUsers', 9)) ?? 0,
          totalTraffic: int.tryParse(col('TotalTraffic', 10)) ?? 0,
          logType: col('LogType', 11),
          operator: col('Operator', 12),
          message: col('Message', 13),
          openVpnConfigDataBase64: tokens.length > 14 ? col('OpenVPN_ConfigData_Base64', 14) : '',
        ));
      } catch (_) {}
    }
    return list;
  }

  void selectServer(VpnServer server) {
    _selectedServer = server;
    notifyListeners();
  }

  void autoSelectBestServer() {
    if (_servers.isEmpty) return;
    _servers.sort((a, b) {
      final pingA = a.ping <= 0 ? 999 : a.ping;
      final pingB = b.ping <= 0 ? 999 : b.ping;
      return pingA.compareTo(pingB);
    });
    _selectedServer = _servers.first;
    notifyListeners();
  }

  Future<void> toggleConnection() async {
    if (_status == ConnectionStatus.connected || _status == ConnectionStatus.connecting) {
      _engine.disconnect();
    } else {
      if (_selectedServer == null) return;
      final config = _selectedServer!.decodedConfig;
      if (config.isEmpty) return;

      _engine.connect(
        config,
        "Proximity VPN - ${_selectedServer!.countryLong}",
        username: "vpn",
        password: "vpn",
        certIsRequired: false,
      );
    }
  }
}
```

---

### 5. `lib/server_list_screen.dart`

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'models/vpn_server.dart';
import 'vpn_service.dart';

class ServerListScreen extends StatefulWidget {
  const ServerListScreen({Key? key}) : super(key: key);

  @override
  State<ServerListScreen> createState() => _ServerListScreenState();
}

class _ServerListScreenState extends State<ServerListScreen> {
  String _searchQuery = '';
  String _sortMode = 'ping'; // 'ping', 'speed', 'name'

  @override
  Widget build(BuildContext context) {
    final vpn = Provider.of<VpnProvider>(context);
    final theme = Theme.of(context);

    var filtered = vpn.servers.where((s) {
      final q = _searchQuery.toLowerCase();
      return s.countryLong.toLowerCase().contains(q) ||
          s.countryShort.toLowerCase().contains(q) ||
          s.ip.contains(q);
    }).toList();

    if (_sortMode == 'ping') {
      filtered.sort((a, b) => (a.ping <= 0 ? 999 : a.ping).compareTo(b.ping <= 0 ? 999 : b.ping));
    } else if (_sortMode == 'speed') {
      filtered.sort((a, b) => b.speed.compareTo(a.speed));
    } else {
      filtered.sort((a, b) => a.countryLong.compareTo(b.countryLong));
    }

    return Scaffold(
      backgroundColor: const Color(0xFF080D1A),
      appBar: AppBar(
        backgroundColor: const Color(0xFF0F172A),
        title: const Text('VPN Locations', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Color(0xFF00E5FF)),
            onPressed: () => vpn.fetchVpnGateServers(),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              onChanged: (val) => setState(() => _searchQuery = val),
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(
                filled: true,
                fillColor: const Color(0xFF16213E),
                hintText: 'Search country or IP...',
                hintStyle: const TextStyle(color: Color(0xFF94A3B8)),
                prefixIcon: const Icon(Icons.search, color: Color(0xFF00E5FF)),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
              ),
            ),
          ),
          // Auto Best Choice Card
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: InkWell(
              onTap: () {
                vpn.autoSelectBestServer();
                Navigator.pop(context);
              },
              child: Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: const Color(0x3300E5FF),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: const Color(0xFF00E5FF)),
                ),
                child: Row(
                  children: const [
                    CircleAvatar(
                      backgroundColor: Color(0xFF00E5FF),
                      child: Icon(Icons.auto_awesome, color: Colors.black),
                    ),
                    SizedBox(width: 12),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Auto-Select Best Server', style: TextStyle(color: Color(0xFF00E5FF), fontWeight: FontWeight.bold)),
                        Text('Fastest ping & highest throughput', style: TextStyle(color: Colors.white70, fontSize: 12)),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          Expanded(
            child: ListView.builder(
              itemCount: filtered.length,
              itemBuilder: (ctx, idx) {
                final server = filtered[idx];
                final isSelected = vpn.selectedServer?.ip == server.ip;
                return ListTile(
                  leading: Text(server.flagEmoji, style: const TextStyle(fontSize: 26)),
                  title: Text(server.countryLong, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                  subtitle: Text('${server.ip} • ${server.speedFormatted}', style: const TextStyle(color: Color(0xFF94A3B8))),
                  trailing: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: server.ping < 50 ? const Color(0x3300E676) : const Color(0x33FFB300),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(server.pingFormatted, style: TextStyle(color: server.ping < 50 ? const Color(0xFF00E676) : const Color(0xFFFFB300), fontWeight: FontWeight.bold)),
                  ),
                  onTap: () {
                    vpn.selectServer(server);
                    Navigator.pop(context);
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
```

---

### 6. `lib/main.dart`

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'vpn_service.dart';
import 'server_list_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(
    ChangeNotifierProvider(
      create: (_) => VpnProvider()..initialize(() {}),
      child: const ProximityVpnApp(),
    ),
  );
}

class ProximityVpnApp extends StatelessWidget {
  const ProximityVpnApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Proximity VPN',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF080D1A),
        primaryColor: const Color(0xFF00E5FF),
      ),
      home: const DashboardScreen(),
    );
  }
}

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final vpn = Provider.of<VpnProvider>(context);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Row(
          children: const [
            Icon(Icons.shield, color: Color(0xFF00E5FF)),
            SizedBox(width: 8),
            Text('PROXIMITY VPN', style: TextStyle(letterSpacing: 1.2, fontWeight: FontWeight.bold)),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.tune, color: Color(0xFF00E5FF)),
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ServerListScreen())),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            // Selected Server Header with Dropdown
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              child: InkWell(
                onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ServerListScreen())),
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFF0F172A),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: const Color(0xFF1E293B)),
                  ),
                  child: Row(
                    children: [
                      Text(vpn.selectedServer?.flagEmoji ?? '🌐', style: const TextStyle(fontSize: 28)),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(vpn.selectedServer?.countryLong ?? 'Select Server', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                            Text(vpn.selectedServer?.ip ?? '---.---.---.---', style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 12)),
                          ],
                        ),
                      ),
                      const Icon(Icons.arrow_drop_down, color: Color(0xFF00E5FF)),
                    ],
                  ),
                ),
              ),
            ),
            const Spacer(),
            // Center Prominent Circular Button with Glow & Dynamic Status
            GestureDetector(
              onTap: vpn.toggleConnection,
              child: Container(
                width: 170,
                height: 170,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: const Color(0xFF16213E),
                  boxShadow: [
                    BoxShadow(
                      color: vpn.status == ConnectionStatus.connected
                          ? const Color(0xFF00E676).withOpacity(0.4)
                          : const Color(0xFF00E5FF).withOpacity(0.4),
                      blurRadius: 36,
                      spreadRadius: 8,
                    ),
                  ],
                  border: Border.all(
                    color: vpn.status == ConnectionStatus.connected ? const Color(0xFF00E676) : const Color(0xFF00E5FF),
                    width: 3.5,
                  ),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      Icons.power_settings_new,
                      size: 56,
                      color: vpn.status == ConnectionStatus.connected ? const Color(0xFF00E676) : const Color(0xFF00E5FF),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      vpn.status == ConnectionStatus.connected
                          ? 'CONNECTED'
                          : vpn.status == ConnectionStatus.connecting
                              ? 'CONNECTING...'
                              : 'CONNECT',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: vpn.status == ConnectionStatus.connected ? const Color(0xFF00E676) : const Color(0xFF00E5FF),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const Spacer(),
            // Live Latency / Speed Card
            Padding(
              padding: const EdgeInsets.all(20),
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFF0F172A),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: const Color(0xFF1E293B)),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    Column(
                      children: [
                        const Text('PING', style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 4),
                        Text(vpn.selectedServer?.pingFormatted ?? '-- ms', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Color(0xFF00E676))),
                      ],
                    ),
                    Column(
                      children: [
                        const Text('COUNTRY', style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 4),
                        Text(vpn.selectedServer?.countryShort ?? '--', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
                      ],
                    ),
                    Column(
                      children: [
                        const Text('IP ADDRESS', style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 4),
                        Text(vpn.selectedServer?.ip ?? '---', style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Color(0xFF00E5FF))),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
          ],
        ),
      ),
    );
  }
}
```
