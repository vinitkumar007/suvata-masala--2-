(function () {
  "use strict";

  var TOKEN_KEY = "smm.admin.token";
  var $ = function (id) { return document.getElementById(id); };
  var inr = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 });
  var money = function (paise) { return inr.format(paise / 100); };
  var token = "";
  var productsCache = [];

  function esc(s) {
    return String(s == null ? "" : s).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  }

  async function api(path, opts) {
    opts = opts || {};
    var headers = { "X-Admin-Token": token };
    if (opts.body !== undefined) headers["Content-Type"] = "application/json";
    var res = await fetch(path, {
      method: opts.method || "GET",
      headers: headers,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined
    });
    var data = await res.json().catch(function () { return {}; });
    if (!res.ok) {
      var err = new Error(data.message || ("Error " + res.status));
      err.status = res.status;
      err.details = data.details || [];
      throw err;
    }
    return data;
  }

  function showApp(on) {
    $("loginBox").hidden = on;
    $("app").hidden = !on;
    $("logout").hidden = !on;
  }

  /* ---------- login ---------- */
  $("loginForm").addEventListener("submit", async function (e) {
    e.preventDefault();
    token = $("token").value.trim();
    $("loginErr").textContent = "";
    try {
      await api("/api/admin/orders");
      try { sessionStorage.setItem(TOKEN_KEY, token); } catch (err) {}
      showApp(true);
      loadOrders();
    } catch (err) {
      token = "";
      $("loginErr").textContent = err.message;
    }
  });

  $("logout").addEventListener("click", function () {
    token = "";
    try { sessionStorage.removeItem(TOKEN_KEY); } catch (err) {}
    showApp(false);
    $("token").value = "";
  });

  /* ---------- tabs ---------- */
  function selectTab(which) {
    var orders = which === "orders";
    $("tabOrders").setAttribute("aria-selected", String(orders));
    $("tabProducts").setAttribute("aria-selected", String(!orders));
    $("ordersPanel").hidden = !orders;
    $("productsPanel").hidden = orders;
    if (!orders) loadProducts();
  }
  $("tabOrders").addEventListener("click", function () { selectTab("orders"); });
  $("tabProducts").addEventListener("click", function () { selectTab("products"); });

  /* ---------- orders ---------- */
  async function loadOrders() {
    $("ordersMsg").textContent = "Orders load ho rahe hain…";
    try {
      var list = await api("/api/admin/orders");
      $("ordersMsg").textContent = list.length + " orders";
      var body = $("ordersBody");
      body.innerHTML = "";
      if (!list.length) {
        body.innerHTML = '<tr><td colspan="6" class="muted">Abhi koi order nahi hai.</td></tr>';
        return;
      }
      list.forEach(function (o) {
        var items = o.items.map(function (i) { return esc(i.productName) + " × " + i.quantity; }).join("<br>");
        var shipCell;
        if (o.status === "PAID") {
          shipCell = '<form class="ship-form" data-ref="' + esc(o.orderRef) + '">' +
            '<input name="tracking" maxlength="60" placeholder="Tracking no." aria-label="Tracking number">' +
            '<button type="submit" class="small primary">Ship</button></form>';
        } else {
          shipCell = o.trackingNumber ? "Tracking: " + esc(o.trackingNumber) : '<span class="small-muted">—</span>';
        }
        var tr = document.createElement("tr");
        tr.innerHTML =
          "<td><strong>" + esc(o.orderRef) + "</strong><div class='small-muted'>" +
            new Date(o.createdAt).toLocaleString("en-IN") + "</div>" +
            (o.razorpayPaymentId ? "<div class='small-muted'>" + esc(o.razorpayPaymentId) + "</div>" : "") + "</td>" +
          '<td><span class="pill ' + esc(o.status) + '">' + esc(o.status) + "</span></td>" +
          "<td>" + esc(o.customerName) + "<div class='small-muted'>" + esc(o.phone) + " · " + esc(o.email) +
            "</div><div class='small-muted'>" + esc(o.deliveryAddress) + "</div></td>" +
          "<td>" + items + "</td>" +
          "<td>" + money(o.totalPaise) + "</td>" +
          "<td>" + shipCell + "</td>";
        body.appendChild(tr);
      });
    } catch (err) {
      $("ordersMsg").textContent = err.message;
    }
  }

  $("ordersBody").addEventListener("submit", async function (e) {
    var form = e.target;
    if (!form.classList.contains("ship-form")) return;
    e.preventDefault();
    var ref = form.getAttribute("data-ref");
    var tracking = form.elements.tracking.value.trim();
    try {
      await api("/api/admin/orders/" + encodeURIComponent(ref) + "/ship", {
        method: "POST",
        body: { trackingNumber: tracking || null }
      });
      $("ordersMsg").textContent = ref + " ship ho gaya.";
      loadOrders();
    } catch (err) {
      $("ordersMsg").textContent = err.message;
    }
  });

  $("refreshOrders").addEventListener("click", loadOrders);

  /* ---------- products ---------- */
  async function loadProducts() {
    $("productsMsg").textContent = "Products load ho rahe hain…";
    try {
      productsCache = await api("/api/admin/products");
      $("productsMsg").textContent = productsCache.length + " products";
      var body = $("productsBody");
      body.innerHTML = "";
      productsCache.forEach(function (p) {
        var tr = document.createElement("tr");
        tr.innerHTML =
          "<td><strong>" + esc(p.name) + "</strong><div class='small-muted'>" + esc(p.slug) + "</div></td>" +
          "<td>" + esc(p.category) + "</td>" +
          "<td>" + p.weightGrams + " g</td>" +
          "<td>" + money(p.pricePaise) + "</td>" +
          "<td>" + (p.inStock ? "Haan" : "<span class='pill CANCELLED'>Nahi</span>") + "</td>" +
          '<td><button type="button" class="small" data-edit="' + p.id + '">Edit</button></td>';
        body.appendChild(tr);
      });
    } catch (err) {
      $("productsMsg").textContent = err.message;
    }
  }

  function openProductForm(p) {
    var f = $("productForm");
    f.reset();
    $("productErr").textContent = "";
    f.elements.id.value = p ? p.id : "";
    $("productFormTitle").textContent = p ? "Edit: " + p.name : "Naya product";
    if (p) {
      f.elements.slug.value = p.slug;
      f.elements.name.value = p.name;
      f.elements.nameHindi.value = p.nameHindi || "";
      f.elements.category.value = p.category;
      f.elements.description.value = p.description;
      f.elements.weightGrams.value = p.weightGrams;
      f.elements.priceRupees.value = (p.pricePaise / 100).toString();
      f.elements.accent.value = p.accent || "";
      f.elements.imageUrl.value = p.imageUrl || "";
      f.elements.inStock.checked = p.inStock;
    } else {
      f.elements.inStock.checked = true;
    }
    f.hidden = false;
    f.elements.slug.focus();
  }

  $("productsBody").addEventListener("click", function (e) {
    var id = e.target.getAttribute("data-edit");
    if (!id) return;
    var p = productsCache.find(function (x) { return String(x.id) === id; });
    if (p) openProductForm(p);
  });
  $("newProduct").addEventListener("click", function () { openProductForm(null); });
  $("cancelProduct").addEventListener("click", function () { $("productForm").hidden = true; });

  $("productForm").addEventListener("submit", async function (e) {
    e.preventDefault();
    var f = e.target;
    var id = f.elements.id.value;
    var rupees = parseFloat(f.elements.priceRupees.value);
    var body = {
      slug: f.elements.slug.value.trim(),
      name: f.elements.name.value.trim(),
      nameHindi: f.elements.nameHindi.value.trim() || null,
      category: f.elements.category.value.trim(),
      description: f.elements.description.value.trim(),
      weightGrams: parseInt(f.elements.weightGrams.value, 10),
      pricePaise: Math.round(rupees * 100),
      inStock: f.elements.inStock.checked,
      accent: f.elements.accent.value.trim() || null,
      imageUrl: f.elements.imageUrl.value.trim() || null
    };
    if (!body.slug || !body.name || !body.category || !body.description || !(rupees > 0) || !(body.weightGrams > 0)) {
      $("productErr").textContent = "Sabhi zaroori fields bharein.";
      return;
    }
    try {
      if (id) {
        await api("/api/admin/products/" + id, { method: "PUT", body: body });
      } else {
        await api("/api/admin/products", { method: "POST", body: body });
      }
      f.hidden = true;
      $("productsMsg").textContent = "Save ho gaya.";
      loadProducts();
    } catch (err) {
      $("productErr").textContent = err.details && err.details.length ? err.details.join(" · ") : err.message;
    }
  });

  /* ---------- start ---------- */
  var saved = "";
  try { saved = sessionStorage.getItem(TOKEN_KEY) || ""; } catch (err) {}
  if (saved) {
    token = saved;
    api("/api/admin/orders").then(function () {
      showApp(true);
      loadOrders();
    }).catch(function () {
      token = "";
      try { sessionStorage.removeItem(TOKEN_KEY); } catch (err) {}
    });
  }
})();
