class ExternalSubtitleData {
  final int sequenceNumber;
  final Duration start;
  final Duration end;
  final String text;

  const ExternalSubtitleData({
    required this.sequenceNumber,
    required this.start,
    required this.end,
    required this.text,
  });

  int compareTo(ExternalSubtitleData other) => start.inMilliseconds.compareTo(other.start.inMilliseconds);

  bool inRange(Duration duration) => start <= duration && end >= duration;
  bool isLarg(Duration duration) => duration > end;
  bool inSmall(Duration duration) => duration < start;

  @override
  bool operator ==(covariant ExternalSubtitleData other) {
    if (identical(this, other)) return true;

    return other.sequenceNumber == sequenceNumber && other.start == start && other.end == end && other.text == text;
  }

  @override
  int get hashCode => sequenceNumber.hashCode ^ start.hashCode ^ end.hashCode ^ text.hashCode;

  @override
  String toString() {
    return 'ExternalSubtitleData(sequenceNumber: $sequenceNumber, start: $start, end: $end, text: $text)';
  }
}
