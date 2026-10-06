import re
import urllib.request

html = urllib.request.urlopen("http://www.fawanews.sc/", timeout=20).read().decode("utf-8", "replace")
blocks = re.split(r'class="user-item', html)
news = None
for b in blocks[1:]:
    playing = re.search(r'user-item__playing">([\s\S]*?)</', b)
    name = re.search(r'user-item__name">([^<]+)', b)
    href = re.search(r'href="([^"]+\.html)"', b)
    if not href or not name:
        continue
    playing_text = re.sub(r"<[^>]+>", "", playing.group(1)).strip() if playing else ""
    if playing_text:
        continue
    news = href.group(1)
    print("NEWS", name.group(1), news)
    break

if not news:
    for b in blocks[1:]:
        name = re.search(r'user-item__name">([^<]+)', b)
        href = re.search(r'href="([^"]+\.html)"', b)
        if href and name and "news" in name.group(1).lower():
            news = href.group(1)
            print("NEWS by title", name.group(1), news)
            break

if not news:
    print("no news found, try any link")
    m = re.search(r'href="([^"]+\.html)"', html)
    news = m.group(1) if m else None

if news:
    url = news if news.startswith("http") else "http://www.fawanews.sc/" + news.lstrip("/")
    page = urllib.request.urlopen(url, timeout=20).read().decode("utf-8", "replace")
    print("URL", url, "len", len(page))
    for pat in ["widjet__body", "article", "uk-article", "post-content", "entry-content"]:
        print(pat, page.find(pat))
    idx = page.find("widjet__body")
    if idx >= 0:
        print(page[idx : idx + 4000])
