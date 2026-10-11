(function () {
  "use strict";

  var CART_KEY = "smm.cart.v1";
  var FREE_SHIPPING_PAISE = 49900;   // server ke application.yml se match karta hai
  var SHIPPING_PAISE = 6000;
  var RAZORPAY_SCRIPT = "https://checkout.razorpay.com/v1/checkout.js";

  var products = [];
  var activeCategory = "Sab";
  var cart = loadCart();

  var $ = function (id) { return document.getElementById(id); };
  var inr = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });
  function money(paise) { return inr.format(paise / 100); }

  /* ---------- cart storage ---------- */
  function loadCart() {
    try {
      var raw = JSON.parse(localStorage.getItem(CART_KEY) || "{}");
      return typeof raw === "object" && raw ? raw : {};
    } catch (e) { return {}; }
  }
  function saveCart() {
    try { localStorage.setItem(CART_KEY, JSON.stringify(cart)); } catch (e) { /* private mode */ }
  }
  function productById(id) {
    for (var i = 0; i < products.length; i++) if (products[i].id === Number(id)) return products[i];
    return null;
  }
  function cartLines() {
    return Object.keys(cart).map(function (id) {
      var p = productById(id);
      return p ? { product: p, qty: cart[id] } : null;
    }).filter(Boolean);
  }
  function cartCount() {
    return Object.keys(cart).reduce(function (n, id) { return n + cart[id]; }, 0);
  }
  function subtotalPaise() {
    return cartLines().reduce(function (s, l) { return s + l.product.pricePaise * l.qty; }, 0);
  }
  function shippingPaise(sub) {
    return sub === 0 || sub >= FREE_SHIPPING_PAISE ? 0 : SHIPPING_PAISE;
  }

  function addToCart(id) {
    var p = productById(id);
    if (!p || !p.inStock) return;
    cart[id] = Math.min((cart[id] || 0) + 1, 20);
    saveCart();
    renderCart();
    toast(p.name + " cart mein add ho gaya");
  }
  function setQty(id, qty) {
    if (qty <= 0) delete cart[id];
    else cart[id] = Math.min(qty, 20);
    saveCart();
    renderCart();
  }

  /* ---------- products ---------- */
  async function loadProducts() {
    try {
      var res = await fetch("/api/products");
      if (!res.ok) throw new Error("HTTP " + res.status);
      products = await res.json();
      $("productStatus").hidden = true;
      renderFilters();
      renderProducts();
      renderCart();
    } catch (e) {
      $("productStatus").textContent = "Products abhi load nahi ho paye. Thodi der baad refresh karein.";
    }
  }

  function renderFilters() {
    var cats = ["Sab"].concat(Array.from(new Set(products.map(function (p) { return p.category; }))));
    var box = $("filters");
    box.innerHTML = "";
    cats.forEach(function (c) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "chip";
      b.textContent = c;
      b.setAttribute("aria-pressed", String(c === activeCategory));
      b.addEventListener("click", function () {
        activeCategory = c;
        renderFilters();
        renderProducts();
      });
      box.appendChild(b);
    });
  }

  function renderProducts() {
    var grid = $("productGrid");
    grid.innerHTML = "";
    products
      .filter(function (p) { return activeCategory === "Sab" || p.category === activeCategory; })
      .forEach(function (p) { grid.appendChild(productCard(p)); });
  }

  function productCard(p) {
    var card = document.createElement("article");
    card.className = "card";

    var tile = document.createElement("div");
    tile.className = "tile";
    tile.style.background = p.accent || "#8c5a2b";
    if (p.imageUrl) {
      var img = document.createElement("img");
      img.src = p.imageUrl;
      img.alt = p.name;
      img.loading = "lazy";
      tile.appendChild(img);
    } else {
      var label = document.createElement("span");
      label.textContent = p.category;
      tile.appendChild(label);
    }

    var body = document.createElement("div");
    body.className = "body";

    var meta = document.createElement("div");
    meta.className = "meta";
    var w = document.createElement("span");
    w.textContent = p.weightGrams + " g";
    meta.appendChild(w);
    if (!p.inStock) {
      var oos = document.createElement("span");
      oos.className = "oos";
      oos.textContent = "Stock khatam";
      meta.appendChild(oos);
    }

    var h = document.createElement("h3");
    h.textContent = p.name;

    var hi = document.createElement("p");
    hi.className = "hindi";
    hi.textContent = p.nameHindi || "";

    var desc = document.createElement("p");
    desc.className = "desc";
    desc.textContent = p.description;

    var buy = document.createElement("div");
    buy.className = "buy";
    var price = document.createElement("span");
    price.className = "price";
    price.textContent = money(p.pricePaise);
    var add = document.createElement("button");
    add.type = "button";
    add.className = "add";
    add.textContent = p.inStock ? "Cart mein daalein" : "Uplabdh nahi";
    add.disabled = !p.inStock;
    add.addEventListener("click", function () { addToCart(p.id); });
    buy.appendChild(price);
    buy.appendChild(add);

    body.appendChild(meta);
    body.appendChild(h);
    body.appendChild(hi);
    body.appendChild(desc);
    body.appendChild(buy);

    card.appendChild(tile);
    card.appendChild(body);
    return card;
  }

  /* ---------- cart panel ---------- */
  function openCart() {
    $("cartPanel").classList.add("open");
    $("cartPanel").setAttribute("aria-hidden", "false");
    $("scrim").hidden = false;
    $("cartBtn").setAttribute("aria-expanded", "true");
    $("cartClose").focus();
  }
  function closeCart() {
    $("cartPanel").classList.remove("open");
    $("cartPanel").setAttribute("aria-hidden", "true");
    $("scrim").hidden = true;
    $("cartBtn").setAttribute("aria-expanded", "false");
  }

  function renderCart() {
    var lines = cartLines();
    var count = cartCount();
    $("cartCount").textContent = String(count);

    var body = $("cartBody");
    body.innerHTML = "";
    if (!lines.length) {
      var empty = document.createElement("p");
      empty.className = "empty";
      empty.textContent = "Cart khaali hai. Masale chunein.";
      body.appendChild(empty);
    }
    lines.forEach(function (l) {
      var row = document.createElement("div");
      row.className = "line";

      var left = document.createElement("div");
      var name = document.createElement("div");
      name.className = "name";
      name.textContent = l.product.name;
      var sub = document.createElement("div");
      sub.className = "sub";
      sub.textContent = l.product.weightGrams + " g · " + money(l.product.pricePaise) + " each";
      left.appendChild(name);
      left.appendChild(sub);

      var right = document.createElement("div");
      right.className = "amt";
      right.textContent = money(l.product.pricePaise * l.qty);

      var controls = document.createElement("div");
      controls.style.display = "flex";
      controls.style.alignItems = "center";
      controls.style.gap = "12px";
      controls.appendChild(qtyControl(l.product.id, l.qty));
      var rm = document.createElement("button");
      rm.type = "button";
      rm.className = "rm";
      rm.textContent = "Hatayein";
      rm.addEventListener("click", function () { setQty(l.product.id, 0); });
      controls.appendChild(rm);

      row.appendChild(left);
      row.appendChild(right);
      row.appendChild(controls);
      body.appendChild(row);
    });

    var sub = subtotalPaise();
    var ship = shippingPaise(sub);
    $("subtotal").textContent = money(sub);
    $("shipping").textContent = lines.length ? (ship === 0 ? "Free" : money(ship)) : "—";
    $("grand").textContent = money(sub + ship);
    $("shipNote").textContent = lines.length && ship > 0
      ? "₹" + (FREE_SHIPPING_PAISE / 100) + " se upar ka order par delivery free hai."
      : "";
    $("checkoutBtn").disabled = !lines.length;
  }

  function qtyControl(id, qty) {
    var wrap = document.createElement("div");
    wrap.className = "qty";
    var minus = document.createElement("button");
    minus.type = "button";
    minus.textContent = "−";
    minus.setAttribute("aria-label", "Ek kam karein");
    minus.addEventListener("click", function () { setQty(id, qty - 1); });
    var val = document.createElement("span");
    val.textContent = String(qty);
    var plus = document.createElement("button");
    plus.type = "button";
    plus.textContent = "+";
    plus.setAttribute("aria-label", "Ek badhayein");
    plus.addEventListener("click", function () { setQty(id, qty + 1); });
    wrap.appendChild(minus);
    wrap.appendChild(val);
    wrap.appendChild(plus);
    return wrap;
  }

  /* ---------- checkout ---------- */
  function toast(msg) {
    var t = $("toast");
    t.textContent = msg;
    t.hidden = false;
    clearTimeout(toast._t);
    toast._t = setTimeout(function () { t.hidden = true; }, 2800);
  }

  function setFieldErrors(form, details) {
    form.querySelectorAll("[aria-invalid]").forEach(function (f) { f.removeAttribute("aria-invalid"); });
    (details || []).forEach(function (d) {
      var field = String(d).split(":")[0].trim();
      var input = form.elements[field];
      if (input) input.setAttribute("aria-invalid", "true");
    });
  }

  function readCheckoutForm(form) {
    var fd = new FormData(form);
    var data = {};
    fd.forEach(function (v, k) { data[k] = String(v).trim(); });
    return data;
  }

  function clientValidate(d) {
    var errs = [];
    if (!d.customerName) errs.push("customerName: naam dein");
    if (!/^[6-9]\d{9}$/.test(d.phone || "")) errs.push("phone: 10 digit ka mobile number dein");
    if (!/^\S+@\S+\.\S+$/.test(d.email || "")) errs.push("email: valid email dein");
    if (!d.addressLine) errs.push("addressLine: pata dein");
    if (!d.city) errs.push("city: shehar dein");
    if (!d.state) errs.push("state: state dein");
    if (!/^\d{6}$/.test(d.pincode || "")) errs.push("pincode: 6 digit ka pincode dein");
    return errs;
  }

  function openCheckout() {
    closeCart();
    $("checkoutMsg").textContent = "";
    $("checkoutDialog").showModal();
  }

  async function submitCheckout(e) {
    e.preventDefault();
    var form = $("checkoutForm");
    var msg = $("checkoutMsg");
    var d = readCheckoutForm(form);
    var errs = clientValidate(d);
    if (errs.length) {
      setFieldErrors(form, errs);
      msg.textContent = "Kripya form ki galtiyan theek karein.";
      return;
    }
    setFieldErrors(form, []);

    var payBtn = $("payBtn");
    payBtn.disabled = true;
    msg.textContent = "Order ban raha hai…";

    var body = {
      customerName: d.customerName,
      email: d.email,
      phone: d.phone,
      addressLine: d.addressLine,
      city: d.city,
      state: d.state,
      pincode: d.pincode,
      items: cartLines().map(function (l) { return { productId: l.product.id, quantity: l.qty }; })
    };

    try {
      var res = await fetch("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      });
      var data = await res.json().catch(function () { return {}; });
      if (!res.ok) {
        setFieldErrors(form, data.details || []);
        msg.textContent = data.message || "Order nahi ban paya. Dobara koshish karein.";
        return;
      }
      msg.textContent = "";
      await loadRazorpay();
      openRazorpay(data, d);
    } catch (err) {
      msg.textContent = "Network mein dikkat hai. Internet check karke dobara koshish karein.";
    } finally {
      payBtn.disabled = false;
    }
  }

  function loadRazorpay() {
    if (window.Razorpay) return Promise.resolve();
    return new Promise(function (resolve, reject) {
      var s = document.createElement("script");
      s.src = RAZORPAY_SCRIPT;
      s.onload = resolve;
      s.onerror = function () { reject(new Error("Razorpay load nahi hua")); };
      document.head.appendChild(s);
    });
  }

  function openRazorpay(order, d) {
    var rzp = new window.Razorpay({
      key: order.razorpayKeyId,
      amount: order.amountPaise,
      currency: order.currency,
      name: "Suvata Marwari Masala",
      description: "Order " + order.orderRef,
      order_id: order.razorpayOrderId,
      prefill: { name: d.customerName, email: d.email, contact: d.phone },
      notes: { orderRef: order.orderRef },
      theme: { color: "#a3291c" },
      handler: function (resp) { verifyPayment(order.orderRef, resp); },
      modal: {
        ondismiss: function () {
          toast("Payment poora nahi hua. Cart mein masale abhi bhi hain.");
        }
      }
    });
    rzp.on("payment.failed", function () {
      toast("Payment fail ho gaya. Dobara koshish karein.");
    });
    $("checkoutDialog").close();
    rzp.open();
  }

  async function verifyPayment(orderRef, resp) {
    try {
      var res = await fetch("/api/payments/verify", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          orderRef: orderRef,
          razorpayOrderId: resp.razorpay_order_id,
          razorpayPaymentId: resp.razorpay_payment_id,
          razorpaySignature: resp.razorpay_signature
        })
      });
      var data = await res.json().catch(function () { return {}; });
      if (!res.ok) {
        toast(data.message || "Payment verify nahi ho paya. Hamse sampark karein.");
        return;
      }
      cart = {};
      saveCart();
      renderCart();
      $("doneRef").textContent = data.orderRef;
      $("doneTotal").textContent = money(data.totalPaise);
      $("doneDialog").showModal();
    } catch (err) {
      toast("Network dikkat. Payment ho gaya ho to order number save karein: check kar lein.");
    }
  }

  /* ---------- contact ---------- */
  async function submitContact(e) {
    e.preventDefault();
    var form = $("contactForm");
    var msg = $("contactMsg");
    var d = readCheckoutForm(form);
    msg.className = "form-msg";
    if (!d.name || !/^\S+@\S+\.\S+$/.test(d.email || "") || (d.message || "").length < 5) {
      msg.textContent = "Naam, valid email aur sandesh zaroori hain.";
      return;
    }
    msg.textContent = "Bhej rahe hain…";
    try {
      var res = await fetch("/api/contact", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: d.name, email: d.email, phone: d.phone || null, message: d.message })
      });
      var data = await res.json().catch(function () { return {}; });
      if (!res.ok) {
        msg.textContent = data.message || "Sandesh nahi gaya. Dobara koshish karein.";
        return;
      }
      msg.className = "form-msg ok";
      msg.textContent = data.message;
      form.reset();
    } catch (err) {
      msg.textContent = "Network dikkat hai. Dobara koshish karein.";
    }
  }

  /* ---------- wire up ---------- */
  $("cartBtn").addEventListener("click", openCart);
  $("cartClose").addEventListener("click", closeCart);
  $("scrim").addEventListener("click", closeCart);
  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape" && $("cartPanel").classList.contains("open")) closeCart();
  });
  $("checkoutBtn").addEventListener("click", openCheckout);
  $("checkoutForm").addEventListener("submit", submitCheckout);
  $("contactForm").addEventListener("submit", submitContact);
  $("year").textContent = String(new Date().getFullYear());

  renderCart();
  loadProducts();
})();
