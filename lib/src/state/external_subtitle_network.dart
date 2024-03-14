part of 'external_subtitle_controller.dart';

class ExternalSubtitleNetwork extends ExternalSubtitleController {
  final Uri url;
  final SubtitleType? type;
  final Duration? connectionTimeout;
  final Map<String, String>? headers;
  final bool Function(X509Certificate cert, String host, int port)? badCertificateCallback;
  final int successHttpStatus;

  const ExternalSubtitleNetwork(
    this.url, {
    this.type,
    this.headers,
    this.connectionTimeout,
    this.successHttpStatus = HttpStatus.ok,
    this.badCertificateCallback,
  });

  @override
  Future<ExternalSubtitleObject> getSubtitle() async {
    final client = HttpClient();
    client.connectionTimeout = connectionTimeout;
    client.badCertificateCallback = badCertificateCallback;
    final request = await client.getUrl(url);
    if (headers != null) {
      headers?.forEach((name, value) {
        request.headers.add(name, value);
      });
    }
    final response = await request.close();
    final responseBody = await response.transform(utf8.decoder).join();
    client.close(force: true);

    return ExternalSubtitleObject(
      data: responseBody,
      type: type ?? ExternalSubtitleController.getSubtitleType(extension(url.path)),
    );
  }
}
