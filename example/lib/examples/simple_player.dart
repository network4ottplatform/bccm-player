import 'package:bccm_player/bccm_player.dart';
import 'package:flutter/material.dart';

class SimplePlayer extends StatefulWidget {
  const SimplePlayer({super.key});

  @override
  State<SimplePlayer> createState() => _SimplePlayerState();
}

class _SimplePlayerState extends State<SimplePlayer> {
  late BccmPlayerViewController viewPlayerController;

  @override
  void initState() {
    viewPlayerController = BccmPlayerViewController(
      playerController: BccmPlayerController(
        MediaItem(
          url: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/adv_dv_atmos/main.m3u8',
          mimeType: 'application/x-mpegURL',
          metadata: MediaMetadata(title: 'Apple advanced (HLS/HDR)'),
        ),
      ),
    );
    viewPlayerController.playerController.initialize();
    viewPlayerController.playerController.play();
    super.initState();
  }

  @override
  void dispose() {
    viewPlayerController.playerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        Column(
          children: [
            BccmPlayerView.withViewController(
              viewPlayerController,
            ),
            ElevatedButton(
              onPressed: () {
                viewPlayerController.playerController.setPrimary();
              },
              child: const Text('Make primary'),
            ),
            ElevatedButton(
              onPressed: () {
                final currentMs = viewPlayerController.playerController.value.playbackPositionMs;
                if (currentMs != null) {
                  viewPlayerController.playerController.seekTo(Duration(milliseconds: currentMs + 20000));
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
