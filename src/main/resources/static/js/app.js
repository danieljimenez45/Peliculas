document.addEventListener('DOMContentLoaded', () => {
  const lnkSalir = document.querySelector('#logoutLink');
  if (lnkSalir) {
    lnkSalir.addEventListener('click', (event) => {
      event.preventDefault();
      document.querySelector('#logoutForm').submit();
    });
  }
});
