part of 'external_subtitle_controller.dart';

class ExternalSubtitleFile extends ExternalSubtitleController {
  final File file;
  final SubtitleType? type;

  const ExternalSubtitleFile(this.file, {this.type});

  @override
  Future<ExternalSubtitleObject> getSubtitle() async {
    final data = await file.readAsString();
    final type = this.type ?? ExternalSubtitleController.getSubtitleType(extension(file.path));
    return ExternalSubtitleObject(data: data, type: type);
  }
}
