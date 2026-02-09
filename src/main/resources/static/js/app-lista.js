document.addEventListener('DOMContentLoaded', () => {

  const container = document.querySelector('#lista-peliculas-container');
  if (!container) return;

  // Delegación de eventos: borrar película (modal)
  container.addEventListener('click', async (event) => {
    const link = event.target.closest('a');
    const isDelete = link && link.classList.contains('borrarPeliculaLink');
    if (!isDelete) return;

    event.preventDefault();

    const tr = link.closest('tr');
    const idEl = tr && tr.querySelector('.peliculaId');
    const id = idEl ? idEl.textContent.trim() : null;

    const url = '/admin/peliculas/' + id + '/delete/confirm';
    try {
      const response = await fetch(url);
      if (!response.ok) throw new Error('Response status: ' + response.status);
      const html = await response.text();
      document.querySelector('#placeholder-modal').innerHTML = html;

      const modalEl = document.querySelector('#delete-modal');
      if (modalEl) {
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
      } else {
        console.error('Modal no encontrado en el HTML recibido');
      }
    } catch (error) {
      console.error(error.message);
    }
  });

  // Buscador por título (solo si existe el input en la página)
  const buscador = document.querySelector('#buscador');
  if (buscador) {
    buscador.addEventListener('keyup', async () => {
      const url = '/admin/peliculas/filter?';
      const queryParams = new URLSearchParams({ titulo: buscador.value }).toString();
      try {
        const response = await fetch(url + queryParams);
        if (!response.ok) throw new Error('Response status: ' + response.status);
        const html = await response.text();
        container.innerHTML = html;
      } catch (error) {
        console.error(error.message);
      }
    });
  }
});
