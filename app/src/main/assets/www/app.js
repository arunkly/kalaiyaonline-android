const ORIGIN = "https://kalaiyaonline.com";
const app = document.querySelector("#app");
const tabs = document.querySelectorAll(".tabbar button");
let screen = "home";

const CATS = [
  ["news", "समाचार"],
  ["local", "स्थानीय"],
  ["international", "अन्तर्राष्ट्रिय"],
  ["politics", "राजनीति"],
  ["artha", "अर्थ"],
  ["khelkud", "खेलकुद"],
  ["entertainment", "मनोरंजन"],
];

function clean(value) {
  return (value || "").replace(/\s+/g, " ").trim();
}

function esc(value) {
  return clean(value).replace(/[&<>"']/g, function (char) {
    if (char === "&") return "&" + "amp;";
    if (char === "<") return "&" + "lt;";
    if (char === ">") return "&" + "gt;";
    if (char === '"') return "&" + "quot;";
    return "&" + "#39;";
  });
}

function fetchHtml(path) {
  const native = globalThis.KalaiyaNative;
  if (!native || typeof native.fetchText !== "function") throw new Error("एप ब्रिज भेटिएन।");
  const html = native.fetchText(new URL(path, ORIGIN).toString());
  if (!html || html.length < 500 || html.includes("One moment, please")) {
    throw new Error("कलैया अनलाइन अहिले लोड हुन सकेन।");
  }
  return new DOMParser().parseFromString(html, "text/html");
}

function storyFrom(article) {
  const link = article.querySelector("a[href*='/news/']");
  if (!link) return null;
  let url;
  try { url = new URL(link.getAttribute("href"), ORIGIN); } catch { return null; }
  const match = url.pathname.match(/\/news\/([^/]+)/);
  if (!match) return null;
  const title = clean(article.querySelector("h1,h2,h3,h4")?.textContent) || clean(link.querySelector("img")?.alt);
  if (title.length < 8) return null;
  const image = [...article.querySelectorAll("img")]
    .map((img) => img.getAttribute("src") || "")
    .find((src) => src.includes("imagekit.io") || src.includes("/uploads/"));
  return { slug: decodeURIComponent(match[1]), title, image: image ? new URL(image, ORIGIN).toString() : "" };
}

function stories(doc) {
  const seen = new Set();
  const list = [];
  doc.querySelectorAll("article").forEach((article) => {
    const story = storyFrom(article);
    if (!story || seen.has(story.slug)) return;
    seen.add(story.slug);
    list.push(story);
  });
  return list;
}

function imgTag(src, tall) {
  if (!src) return `<div style="height:${tall ? 210 : 120}px;background:#e8f6ee"></div>`;
  return `<img src="${src.replace(/"/g, "")}" alt="" />`;
}

function setTab(name) {
  tabs.forEach((button) => button.classList.toggle("on", button.dataset.tab === name));
}

function render(html) {
  app.innerHTML = html;
  app.querySelectorAll("[data-open]").forEach((node) => {
    node.addEventListener("click", () => openArticle(node.dataset.open));
  });
}

async function showHome() {
  screen = "home";
  setTab("home");
  render(`<p class="empty">समाचार लोड हुँदैछ…</p>`);
  try {
    const list = stories(fetchHtml("/"));
    const lead = list[0];
    const rest = list.slice(1, 13);
    const chips = CATS.map(([slug, label]) => `<button type="button" data-cat="${slug}">${label}</button>`).join("");
    render(`
      <div class="chips">${chips}</div>
      ${lead ? `<button class="hero" data-open="${esc(lead.slug)}">${imgTag(lead.image, true)}<div class="pad"><p class="kicker">मुख्य समाचार</p><h1>${esc(lead.title)}</h1></div></button>` : ""}
      <div class="row">${rest.map((item) => `<button class="card" data-open="${esc(item.slug)}">${imgTag(item.image, false)}<div class="pad"><h2>${esc(item.title)}</h2></div></button>`).join("")}</div>
    `);
    app.querySelectorAll("[data-open]").forEach((node) => node.addEventListener("click", () => openArticle(node.dataset.open)));
    app.querySelectorAll("[data-cat]").forEach((node) => node.addEventListener("click", () => showCategory(node.dataset.cat, node.textContent)));
  } catch (error) {
    render(`<p class="empty">${error.message}</p>`);
  }
}

async function showCategory(slug, label) {
  screen = "category";
  setTab("home");
  render(`<p class="empty">${label} लोड हुँदैछ…</p>`);
  try {
    const list = stories(fetchHtml(`/category/${slug}`)).slice(0, 20);
    render(`<h1 style="margin-bottom:12px">${esc(label)}</h1>` + list.map((item) => `<button class="card" data-open="${esc(item.slug)}">${imgTag(item.image, false)}<div class="pad"><h2>${esc(item.title)}</h2></div></button>`).join(""));
    app.querySelectorAll("[data-open]").forEach((node) => node.addEventListener("click", () => openArticle(node.dataset.open)));
  } catch (error) {
    render(`<p class="empty">${error.message}</p>`);
  }
}

async function openArticle(slug) {
  screen = "article";
  setTab("home");
  render(`<p class="empty">समाचार खुल्दैछ…</p>`);
  try {
    const doc = fetchHtml(`/news/${encodeURIComponent(slug)}`);
    const title = clean(doc.querySelector("h1")?.textContent) || "समाचार";
    const image = doc.querySelector("figure img, meta[property='og:image']")?.getAttribute("src")
      || doc.querySelector("meta[property='og:image']")?.getAttribute("content")
      || "";
    const paragraphs = [...doc.querySelectorAll(".article-content p")]
      .map((node) => clean(node.textContent))
      .filter((text) => text.length > 25);
    const page = `${ORIGIN}/news/${slug}`;
    render(`
      <article class="article">
        ${image ? imgTag(new URL(image, ORIGIN).toString(), true) : ""}
        <h1 style="margin:12px 0">${esc(title)}</h1>
        ${paragraphs.map((text) => `<p>${esc(text)}</p>`).join("") || `<p class="empty">पूर्ण पाठ भेटिएन।</p>`}
        <button class="share" type="button" id="share">सेयर</button>
      </article>
    `);
    document.querySelector("#share").onclick = () => {
      const native = globalThis.KalaiyaNative;
      if (native?.share) native.share(title, `${title}\n${page}`);
    };
  } catch (error) {
    render(`<p class="empty">${error.message}</p>`);
  }
}

async function showGallery() {
  screen = "gallery";
  setTab("gallery");
  render(`<p class="empty">फोटो लोड हुँदैछ…</p>`);
  try {
    const doc = fetchHtml("/gallery");
    const cards = [];
    const seen = new Set();
    doc.querySelectorAll("a.gallery-card").forEach((node) => {
      const href = node.getAttribute("href") || "";
      if (seen.has(href)) return;
      seen.add(href);
      const title = clean(node.querySelector("b")?.textContent);
      const image = node.querySelector("img")?.getAttribute("src") || "";
      if (title) cards.push({ href, title, image });
    });
    render(`<h1 style="margin-bottom:12px">फोटो ग्यालरी</h1><div class="row">${cards.map((card, index) => `<button class="photo" data-g="${index}">${imgTag(card.image, false)}<div class="pad"><h2>${esc(card.title)}</h2></div></button>`).join("")}</div>`);
    app.querySelectorAll("[data-g]").forEach((node) => node.addEventListener("click", () => openAlbum(cards[Number(node.dataset.g)])));
  } catch (error) {
    render(`<p class="empty">${error.message}</p>`);
  }
}

async function openAlbum(card) {
  screen = "album";
  setTab("gallery");
  render(`<p class="empty">तस्बिर लोड हुँदैछ…</p>`);
  try {
    const doc = fetchHtml(card.href);
    const photos = [...doc.querySelectorAll(".photo-item img")].map((img) => img.getAttribute("src")).filter(Boolean);
    render(`<h1 style="margin-bottom:12px">${esc(card.title)}</h1><div class="row">${photos.map((src) => `<div class="photo">${imgTag(src, false)}</div>`).join("")}</div>`);
  } catch (error) {
    render(`<p class="empty">${error.message}</p>`);
  }
}

async function showDirectory() {
  screen = "directory";
  setTab("directory");
  render(`<p class="empty">डाइरेक्टरी लोड हुँदैछ…</p>`);
  try {
    const doc = fetchHtml("/?directory=all");
    const seen = new Set();
    const cards = [];
    doc.querySelectorAll("a[href*='/directory/']").forEach((node) => {
      const title = clean(node.querySelector("h2,h3,b")?.textContent) || clean(node.querySelector("img")?.alt);
      if (!title || title.length < 2 || seen.has(title)) return;
      seen.add(title);
      const image = node.querySelector("img")?.getAttribute("src") || "";
      cards.push({ title, image });
    });
    render(`<h1 style="margin-bottom:12px">डाइरेक्टरी</h1>` + (cards.length ? cards.map((card) => `<div class="dir">${imgTag(card.image, false)}<div class="pad"><h2>${esc(card.title)}</h2></div></div>`).join("") : `<p class="empty">सूची भेटिएन।</p>`));
  } catch (error) {
    render(`<p class="empty">${error.message}</p>`);
  }
}

tabs.forEach((button) => {
  button.addEventListener("click", () => {
    if (button.dataset.tab === "home") showHome();
    if (button.dataset.tab === "gallery") showGallery();
    if (button.dataset.tab === "directory") showDirectory();
  });
});

window.appBack = function () {
  if (screen === "article" || screen === "category") { showHome(); return "stay"; }
  if (screen === "album") { showGallery(); return "stay"; }
  if (screen !== "home") { showHome(); return "stay"; }
  return "exit";
};

showHome();
