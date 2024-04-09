
enum MetadataNamespace: String {
    // Using namespaces because if we used e.g. json it would be painful to deserialize if we add any large values in the future
    case BccmPlayer = "media.bcc.player"
    case BccmExtras = "media.bcc.extras"
}

enum PlayerMetadataConstants {
    // TODO: refactor to use a PlayerData class instead of serializing into string dicts [String: String]
    static let MimeType = "mime_type"
    static let IsLive = "is_live"
    static let IsOffline = "is_offline"
    static let ArtworkUri = "artwork_uri"
    static let CastUrl = "cast_url"
    static let CastMimeType = "cast_mime_type"
    
}
