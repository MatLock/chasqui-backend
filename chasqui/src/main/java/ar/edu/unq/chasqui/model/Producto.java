package ar.edu.unq.chasqui.model;

import java.util.ArrayList;
import java.util.List;

public class Producto {

	private Integer id;
	private String nombre;
	private Categoria categoria;
	private List<Caracteristica> caracteristicas;
	private Fabricante fabricante;
	private List<Variante> variantes;
	
	//GETs & SETs
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getNombre() {
		return nombre;
	}
	
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	public Categoria getCategoria() {
		return categoria;
	}
	
	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}
	
	public List<Caracteristica> getCaracteristicas() {
		return caracteristicas;
	}
	
	public Fabricante getFabricante() {
		return fabricante;
	}
	
	public void setFabricante(Fabricante fabricante) {
		this.fabricante = fabricante;
	}
	
	public List<Variante> getVariantes() {
		return variantes;
	}
	
	public void setVariantes(List<Variante> variantes) {
		this.variantes = variantes;
	}
	
	
	public void setCaracteristicas(List<Caracteristica> caracteristica) {
		this.caracteristicas = caracteristica;
	}
	
	
		
	//METHODS
	


	public static Producto crearProductoEmpty(){
		Producto p = new Producto();
		p.setVariantes(new ArrayList<Variante>());
		p.setCaracteristicas(new ArrayList<Caracteristica>());
		return p;
	}
	
	
	public boolean tieneVarianteDestacada(){
		for(Variante v : variantes){
			if(v.getDestacado()){
				return true;
			}
		}
		return false;
	}


	
}
