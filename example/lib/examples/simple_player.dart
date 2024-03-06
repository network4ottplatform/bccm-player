import 'package:bccm_player/bccm_player.dart';
import 'package:flutter/material.dart';

class SimplePlayer extends StatefulWidget {
  const SimplePlayer({super.key});

  @override
  State<SimplePlayer> createState() => _SimplePlayerState();
}

class _SimplePlayerState extends State<SimplePlayer> {
  late BccmPlayerController playerController;

  @override
  void initState() {
    playerController = BccmPlayerController(
      MediaItem(
        url: 'https://h.m.cdn.klikfilm.net/premiere_monster/mp4dash/stream_widevine.mpd',
        mimeType: 'application/dash+xml',
        metadata: MediaMetadata(title: 'Apple advanced (HLS/HDR)'),
        drm: MediaDrmConfiguration(
          licenseUrl: 'https://drm-widevine-licensing.axprod.net/AcquireLicense',
          headers: {
            'X-AxDRM-Message':
                'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ2ZXJzaW9uIjoxLCJjb21fa2V5X2lkIjoiMTc0MzNiZmUtZGQ0OS00ZGU3LWE5ZjQtYTgwNDAwNjEwYjE2IiwiYmVnaW5fZGF0ZSI6IjIwMjQtMDMtMDVUMTU6Mzg6MDcrMDc6MDAiLCJleHBpcmF0aW9uX2RhdGUiOiIyMDI0LTAzLTA5VDE1OjM4OjA3KzA3OjAwIiwibWVzc2FnZSI6eyJ0eXBlIjoiZW50aXRsZW1lbnRfbWVzc2FnZSIsInZlcnNpb24iOjIsImxpY2Vuc2UiOnsic3RhcnRfZGF0ZXRpbWUiOiIyMDI0LTAzLTA1VDE1OjM4OjA3KzA3OjAwIiwiZXhwaXJhdGlvbl9kYXRldGltZSI6IjIwMjQtMDMtMDlUMTU6Mzg6MDcrMDc6MDAiLCJhbGxvd19wZXJzaXN0ZW5jZSI6dHJ1ZX0sImNvbnRlbnRfa2V5c19zb3VyY2UiOnsiaW5saW5lIjpbeyJpZCI6IjdlM2E1N2IwLTgyYWMtNDM5ZC04Yzk4LTk2MjYwN2U3NTQ1NSIsIml2IjoiWlVsUTNMc0daNmVWeHFibXRzaUV2Zz09In1dfX19.6vnY_V5FOX0eH9dH4OMGVMTedVBikX68PJEjqFFyHCM'
          },
        ),
      ),
    );
    playerController.initialize().then((_) => playerController.setMixWithOthers(true)); // if you want to play together with other videos
    super.initState();
  }

  @override
  void dispose() {
    playerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        Column(
          children: [
            BccmPlayerView(playerController),
            ElevatedButton(
              onPressed: () {
                playerController.setPrimary();
              },
              child: const Text('Make primary'),
            ),
            ElevatedButton(
              onPressed: () {
                final currentMs = playerController.value.playbackPositionMs;
                if (currentMs != null) {
                  playerController.seekTo(Duration(milliseconds: currentMs + 20000));
                }
              },
              child: const Text('Skip 20 seconds'),
            ),
          ],
        ),
      ],
    );
  }
}
