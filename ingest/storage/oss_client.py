"""Alibaba Cloud OSS client — image download + upload."""
import uuid
import imghdr
from pathlib import Path
import httpx
import oss2
from settings import settings


class OssClient:
    """Download external images → upload to OSS → return OSS URLs."""

    def __init__(self):
        self._configured = bool(
            settings.OSS_ACCESS_KEY_ID
            and settings.OSS_ACCESS_KEY_SECRET
            and settings.OSS_BUCKET_NAME
        )
        if self._configured:
            auth = oss2.Auth(
                settings.OSS_ACCESS_KEY_ID,
                settings.OSS_ACCESS_KEY_SECRET,
            )
            self._bucket = oss2.Bucket(
                auth, settings.OSS_ENDPOINT, settings.OSS_BUCKET_NAME
            )
        self._http = httpx.Client(timeout=30)

    # ============================================================
    # Public API
    # ============================================================

    def upload_article_images(self, content_html: str, cover_url: str, article_dir: str) -> tuple:
        """Process all images in an article.

        Returns (new_content_html, new_cover_url).
        Without OSS credentials, returns original URLs unchanged.
        """
        if not self._configured:
            return content_html, cover_url   # 未配置OSS → 保持原URL

        import re
        img_pattern = re.compile(r'<img[^>]+src=["\']([^"\']+)["\']', re.IGNORECASE)

        def replace_img(match):
            old_url = match.group(1)
            new_url = self._download_and_upload(old_url, article_dir)
            return match.group(0).replace(old_url, new_url)

        new_content = img_pattern.sub(replace_img, content_html)
        new_cover = self._download_and_upload(cover_url, article_dir) if cover_url else cover_url

        return new_content, new_cover

    def _download_and_upload(self, image_url: str, article_dir: str) -> str:
        """Download ONE image → upload to OSS → return OSS URL.

        Returns original URL on any failure.
        """
        try:
            resp = self._http.get(image_url, follow_redirects=True)
            resp.raise_for_status()
            image_bytes = resp.content

            if len(image_bytes) == 0:
                return image_url

            # 推断扩展名
            ext = self._guess_extension(image_bytes, image_url)
            filename = f"{uuid.uuid4().hex}{ext}"
            oss_path = f"{settings.OSS_ARTICLE_DIR}/{article_dir}/{filename}"

            self._bucket.put_object(oss_path, image_bytes)
            return self._public_url(oss_path)

        except Exception:
            return image_url   # 下载/上传失败 → 保留原始URL

    # ============================================================
    # 内部
    # ============================================================

    def _public_url(self, oss_path: str) -> str:
        return f"https://{settings.OSS_BUCKET_NAME}.{settings.OSS_ENDPOINT}/{oss_path}"

    @staticmethod
    def _guess_extension(data: bytes, fallback_url: str) -> str:
        """Detect image type from bytes, fallback to URL suffix."""
        fmt = imghdr.what(None, h=data)
        if fmt:
            return f".{fmt}"
        # fallback: from URL
        path = fallback_url.split("?")[0]
        suffix = Path(path).suffix.lower()
        if suffix in (".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"):
            return suffix
        return ".jpg"
