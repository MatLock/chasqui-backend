package ar.edu.unq.chasqui.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.unq.chasqui.dao.ProductoDAO;
import ar.edu.unq.chasqui.model.Imagen;
import ar.edu.unq.chasqui.model.Producto;
import ar.edu.unq.chasqui.model.Variante;
import ar.edu.unq.chasqui.services.interfaces.ProductoService;

public class ProductoServiceImpl implements ProductoService {

	@Autowired
	private ProductoDAO productoDAO;
	
	
	@Override
	public List<Variante> obtenerVariantesPorCategoria(Integer idCategoria, Integer pagina,
			Integer cantidadDeItems) {
		return productoDAO.obtenerVariantesPorCategoria(idCategoria,pagina, cantidadDeItems);
	}

	@Override
	public List<Variante> obtenerVariantesPorProductor(Integer idProductor, Integer pagina, Integer cantItems) {
		return productoDAO.obtenerVariantesPorProductor(idProductor, pagina, cantItems);
	}
	
	@Override
	public List<Variante> obtenerVariantesPorMedalla(Integer idMedalla, Integer pagina, Integer cantItems) {
		return productoDAO.obtenerVariantesPorMedalla(idMedalla, pagina, cantItems);
	}

	@Override
	public List<Imagen> obtenerImagenesDe(Integer idProducto) {
		return productoDAO.obtenerImagenesDe(idProducto);
	}

}