/**
 * Safe rendering boundary for untrusted .eml message bodies.
 *
 * The upstream EML parser and attachment handling remain unchanged. Only
 * the resulting message body is handled here. HTML is copied through a
 * deliberately small element allowlist into an opaque-origin sandbox with
 * no scripts, network requests, forms, links or embedded frames.
 */
(function (global) {
  'use strict';

  var ALLOWED = new Set([
    'p', 'div', 'span', 'br', 'b', 'strong', 'i', 'em', 'u', 's', 'del',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'pre', 'code',
    'ul', 'ol', 'li', 'table', 'thead', 'tbody', 'tfoot', 'tr',
    'td', 'th', 'hr', 'img', 'sup', 'sub'
  ]);
  var DROP_CONTENT = new Set([
    'script', 'style', 'link', 'meta', 'iframe', 'frame', 'frameset',
    'object', 'embed', 'form', 'input', 'button', 'textarea', 'select',
    'svg', 'math', 'template', 'base', 'noscript', 'video', 'audio',
    'canvas', 'applet'
  ]);
  var MAX_HTML_CHARS = 1024 * 1024;
  var MAX_IMAGE_URI_CHARS = 400000;

  function copySafe(source, parent, depth) {
    if (depth > 64) return;
    if (source.nodeType === 3) {
      parent.appendChild(document.createTextNode(source.nodeValue || ''));
      return;
    }
    if (source.nodeType !== 1) return;

    var tag = source.tagName.toLowerCase();
    if (DROP_CONTENT.has(tag)) return;
    if (!ALLOWED.has(tag)) {
      Array.from(source.childNodes).forEach(function (child) {
        copySafe(child, parent, depth + 1);
      });
      return;
    }

    if (tag === 'img') {
      var src = source.getAttribute('src') || '';
      if (src.length > MAX_IMAGE_URI_CHARS ||
        !/^data:image\/(?:png|jpeg|gif|webp);base64,[a-z0-9+/=\s]+$/i.test(src)) {
        parent.appendChild(document.createTextNode(source.getAttribute('alt') || '[已阻止加载邮件图片]'));
        return;
      }
    }

    var target = document.createElement(tag);
    if (tag === 'img') {
      target.setAttribute('src', source.getAttribute('src'));
      target.setAttribute('alt', source.getAttribute('alt') || '');
    }
    // Only presentation attributes that cannot navigate, execute or fetch.
    if (tag === 'td' || tag === 'th') {
      ['colspan', 'rowspan'].forEach(function (attr) {
        var count = Number(source.getAttribute(attr));
        if (Number.isInteger(count) && count > 0 && count <= 100) {
          target.setAttribute(attr, String(count));
        }
      });
    }
    var dir = source.getAttribute('dir');
    if (dir === 'rtl' || dir === 'ltr') target.setAttribute('dir', dir);
    Array.from(source.childNodes).forEach(function (child) {
      copySafe(child, target, depth + 1);
    });
    parent.appendChild(target);
  }

  function sanitizedHtml(source) {
    // <template> is inert: only allowlisted nodes are cloned out.
    var template = document.createElement('template');
    template.innerHTML = source;
    var result = document.createElement('div');
    Array.from(template.content.childNodes).forEach(function (node) {
      copySafe(node, result, 0);
    });
    return result.innerHTML;
  }

  function render(container, mail) {
    var plain = (mail && typeof mail.text === 'string') ? mail.text : '';
    var html = (mail && typeof mail.html === 'string') ? mail.html : '';
    container.textContent = '';
    container.style.whiteSpace = 'pre-wrap';
    container.style.overflowWrap = 'anywhere';

    if (html.trim() && html.length <= MAX_HTML_CHARS) {
      try {
        var body = sanitizedHtml(html);
        if (body.trim()) {
          var frame = document.createElement('iframe');
          frame.setAttribute('sandbox', '');
          frame.setAttribute('referrerpolicy', 'no-referrer');
          frame.setAttribute('title', '邮件 HTML 正文（隔离预览）');
          frame.style.width = '100%';
          frame.style.height = '60vh';
          frame.style.minHeight = '300px';
          frame.style.border = '0';
          var csp = "default-src 'none'; script-src 'none'; connect-src 'none'; " +
            "img-src data:; style-src 'unsafe-inline'; font-src data:; " +
            "base-uri 'none'; form-action 'none'; object-src 'none'; frame-src 'none'";
          frame.srcdoc = '<!doctype html><html><head><meta charset="utf-8">' +
            '<meta http-equiv="Content-Security-Policy" content="' + csp + '">' +
            '<style>body{margin:12px;font:14px/1.6 system-ui,Arial,sans-serif;' +
            'overflow-wrap:anywhere;}table{border-collapse:collapse;}' +
            'td,th{border:1px solid #ddd;padding:4px;}' +
            'img{max-width:100%;height:auto;}pre{white-space:pre-wrap;}</style>' +
            '</head><body>' + body + '</body></html>';
          container.appendChild(frame);
          return;
        }
      } catch (error) {
        // Bad HTML or a browser policy mismatch must not make EML unreadable.
        // Never log the content or the source URL.
      }
    }

    container.textContent = plain || '[邮件没有可显示的纯文本正文]';
  }

  global.KkSafeMailBody = Object.freeze({ render: render });
})(window);
