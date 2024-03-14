import '../subtitle_data.dart';

class ExternalSubtitleObject {
  final String data;
  final SubtitleType type;

  const ExternalSubtitleObject({
    required this.data,
    required this.type,
  });

  @override
  bool operator ==(covariant ExternalSubtitleObject other) {
    if (identical(this, other)) return true;

    return other.data == data && other.type == type;
  }

  @override
  int get hashCode => data.hashCode ^ type.hashCode;
}

extension ExternalSubtitleObjectExt on ExternalSubtitleObject {
  List<ExternalSubtitleData> extractSubtitles() {
    final String value = data.split('\n').map((line) => line.trim()).toList().join('\n');
    final RegExp subtitleRegExp = RegExp(r'(\d+)\n(\d+:\d+:\d+,\d+) --> (\d+:\d+:\d+,\d+)\n(.+?)(?=\n\n|\n$)', dotAll: true);
    return subtitleRegExp.allMatches(value).map((match) {
      final sequenceNumber = int.parse(match.group(1)!);
      final startTime = _parseDuration(match.group(2)!);
      final endTime = _parseDuration(match.group(3)!);
      final text = match.group(4)!;

      return ExternalSubtitleData(sequenceNumber: sequenceNumber, text: text, start: startTime, end: endTime);
    }).toList();
  }

  Duration _parseDuration(String time) {
    final parts = time.split(':');
    final secondsAndMillis = parts[2].split(',');

    return Duration(
      hours: int.parse(parts[0]),
      minutes: int.parse(parts[1]),
      seconds: int.parse(secondsAndMillis[0]),
      milliseconds: int.parse(secondsAndMillis[1]),
    );
  }
}
