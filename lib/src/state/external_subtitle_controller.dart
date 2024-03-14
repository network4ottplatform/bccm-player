import 'dart:convert';
import 'dart:io';

import 'package:bccm_player/src/model/subtitles/external_subtitle_object.dart';
import 'package:path/path.dart' show extension;
import 'package:bccm_player/src/model/subtitle_data.dart';

part 'external_subtitle_network.dart';
part 'external_subtitle_file.dart';

abstract class ExternalSubtitleController {
  const ExternalSubtitleController();

  factory ExternalSubtitleController.fromNetwork(
    Uri url, {
    SubtitleType? type,
  }) {
    return ExternalSubtitleNetwork(url, type: type);
  }

  factory ExternalSubtitleController.fromFile(
    File file, {
    SubtitleType? type,
  }) {
    return ExternalSubtitleFile(file, type: type);
  }

  Future<ExternalSubtitleObject> getSubtitle();

  static SubtitleType getSubtitleType(String value) {
    switch (value) {
      case '.srt':
        return SubtitleType.srt;
      case '.vtt':
        return SubtitleType.vtt;
      default:
        throw UnimplementedError();
    }
  }
}
