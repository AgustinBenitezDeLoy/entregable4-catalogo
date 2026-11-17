'use strict';
const etiquetas = { ACCION: 'Acción', AVENTURA: 'Aventura', ESTRATEGIA: 'Estrategia', DEPORTES: 'Deportes', ROL: 'Rol' };
const elemento = id => document.getElementById(id);
async function solicitar(ruta, opciones) {
  const respuesta = await fetch(ruta, opciones);
  const datos = await respuesta.json();
  if (!respuesta.ok) throw new Error(datos.error || 'No se pudo completar la solicitud.');
  return datos;
}
async function cargar() {
  const juegos = await solicitar('/api/videojuegos' + (elemento('filtro').value ? '?genero=' + encodeURIComponent(elemento('filtro').value) : ''));
  elemento('juegos').replaceChildren();
  elemento('cantidad').textContent = juegos.length ? `${juegos.length} videojuego(s)` : 'No hay videojuegos para mostrar. Registrá uno o cambiá el filtro.';
  juegos.forEach(juego => {
    const articulo = document.createElement('article');
    const imagen = document.createElement('img');
    imagen.src = juego.imagen;
    imagen.alt = 'Ilustración del videojuego ' + juego.nombre;
    const titulo = document.createElement('h3');
    titulo.textContent = juego.nombre;
    const genero = document.createElement('p');
    genero.textContent = etiquetas[juego.genero];
    articulo.append(imagen, titulo, genero);
    elemento('juegos').append(articulo);
  });
}
function error(e) { elemento('mensaje').textContent = e.message; }
elemento('formulario').addEventListener('submit', async evento => {
  evento.preventDefault();
  const boton = evento.target.querySelector('button');
  boton.disabled = true;
  try {
    await solicitar('/api/videojuegos', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ nombre: elemento('nombre').value, genero: elemento('genero').value, imagen: elemento('imagen').value }) });
    evento.target.reset();
    elemento('filtro').value = '';
    await cargar();
    elemento('mensaje').textContent = 'Videojuego registrado.';
  } catch (e) { error(e); } finally { boton.disabled = false; }
});
elemento('filtro').addEventListener('change', () => cargar().catch(error));
async function iniciar() {
  const [generos, estado] = await Promise.all([solicitar('/api/generos'), solicitar('/api/estado')]);
  for (const id of ['genero', 'filtro']) {
    generos.forEach(genero => elemento(id).add(new Option(etiquetas[genero], genero)));
  }
  elemento('titulo').textContent = estado.mensaje;
  elemento('revision').textContent = 'Versión ' + estado.revision;
  await cargar();
}
iniciar().catch(error);
