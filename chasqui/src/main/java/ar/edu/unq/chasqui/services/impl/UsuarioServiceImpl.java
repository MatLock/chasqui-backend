package ar.edu.unq.chasqui.services.impl;

import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.unq.chasqui.dao.UsuarioDAO;
import ar.edu.unq.chasqui.model.Usuario;
import ar.edu.unq.chasqui.services.interfaces.UsuarioService;

public class UsuarioServiceImpl implements UsuarioService{

	@Autowired
	private UsuarioDAO usuarioDAO;
	
	public Usuario obtenerUsuarioPorID(Integer id) {
		return usuarioDAO.obtenerUsuarioPorID(id);
	}

	public void guardarUsuario(Usuario u) {
		usuarioDAO.guardarUsuario(u);
		
	}

}