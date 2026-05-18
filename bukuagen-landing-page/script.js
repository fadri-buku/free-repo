document.getElementById("year").textContent = new Date().getFullYear();

document.querySelectorAll("[data-buy]").forEach((el) => {
  el.addEventListener("click", (e) => {
    e.preventDefault();
    const plan = el.dataset.buy;
    alert(
      `Thanks — you picked the "${plan}" pre-order.\n\nThis demo doesn't take payments yet; wire up your checkout (Stripe, Shopify, etc.) at this point.`,
    );
  });
});
