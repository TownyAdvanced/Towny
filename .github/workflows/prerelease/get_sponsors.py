from urllib.request import Request, urlopen
from html.parser import HTMLParser

FILE_NAME = "sponsors.txt"
BASE_URL = "https://github.com/sponsors/LlmDl/sponsors_partial?filter=active&page={page}"


class SponsorsParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.results = []

        self.in_block = False
        self.block_depth = 0

        self.block_public_user = None
        self.block_is_private = False

        self.capture_tooltip = False
        self.tooltip_text = []

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        class_attr = attrs.get("class", "")

        if tag == "div" and "d-flex" in class_attr and "mb-1" in class_attr and "mr-1" in class_attr:
            self.in_block = True
            self.block_depth = 1
            self.block_public_user = None
            self.block_is_private = False
            self.capture_tooltip = False
            self.tooltip_text = []
            return

        if not self.in_block:
            return

        if tag == "div":
            self.block_depth += 1
            return

        if tag == "a":
            href = attrs.get("href", "").strip()

            if href.startswith("/") and not href.startswith("//"):
                username = href.lstrip("/").strip()
                if username:
                    self.block_public_user = username
            return

        if tag == "svg":
            if attrs.get("aria-label") == "Private Sponsor":
                self.block_is_private = True
            return

        if tag == "tool-tip":
            self.capture_tooltip = True
            self.tooltip_text = []
            return

    def handle_data(self, data):
        if self.capture_tooltip:
            self.tooltip_text.append(data)

    def handle_endtag(self, tag):
        if not self.in_block:
            return

        if tag == "tool-tip":
            text = "".join(self.tooltip_text).strip()
            if text == "Private Sponsor":
                self.block_is_private = True
            self.capture_tooltip = False
            self.tooltip_text = []
            return

        if tag == "div":
            self.block_depth -= 1

            if self.block_depth == 0:
                if self.block_public_user:
                    self.results.append(self.block_public_user)
                elif self.block_is_private:
                    self.results.append("*privateSponsor")

                self.in_block = False
                self.block_public_user = None
                self.block_is_private = False
                self.capture_tooltip = False
                self.tooltip_text = []


def fetch_html(url):
    req = Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urlopen(req, timeout=30) as response:
        return response.read().decode("utf-8", errors="ignore")


def main():
    public_sponsors = []
    private_sponsors = []
    page = 1

    while True:
        url = BASE_URL.format(page=page)

        try:
            html = fetch_html(url)
        except Exception:
            break

        if not html.strip():
            break

        parser = SponsorsParser()
        parser.feed(html)

        if not parser.results:
            break

        for sponsor in parser.results:
            if sponsor == "*privateSponsor":
                private_sponsors.append(sponsor)
            else:
                public_sponsors.append(sponsor)

        page += 1

    sponsors = public_sponsors + private_sponsors

    with open(FILE_NAME, "w", encoding="utf-8") as f:
        f.write("\n".join(sponsors))
        if sponsors:
            f.write("\n")

    print(f"Public sponsors: {len(public_sponsors)}")
    print(f"Private sponsors: {len(private_sponsors)}")
    print(f"Total lines: {len(sponsors)}")
    print(f"Wrote {FILE_NAME}")


if __name__ == "__main__":
    main()
