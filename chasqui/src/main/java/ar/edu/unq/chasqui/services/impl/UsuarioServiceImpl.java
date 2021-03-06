package ar.edu.unq.chasqui.services.impl;

import java.util.List;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.cxf.common.util.StringUtils;
import org.hsqldb.lib.StringUtil;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.unq.chasqui.aspect.Auditada;
import ar.edu.unq.chasqui.dao.UsuarioDAO;
import ar.edu.unq.chasqui.exceptions.DireccionesInexistentes;
import ar.edu.unq.chasqui.exceptions.PedidoInexistenteException;
import ar.edu.unq.chasqui.exceptions.PedidoVigenteException;
import ar.edu.unq.chasqui.exceptions.ProductoInexsistenteException;
import ar.edu.unq.chasqui.exceptions.RequestIncorrectoException;
import ar.edu.unq.chasqui.exceptions.UsuarioExistenteException;
import ar.edu.unq.chasqui.model.Cliente;
import ar.edu.unq.chasqui.model.Direccion;
import ar.edu.unq.chasqui.model.Imagen;
import ar.edu.unq.chasqui.model.Notificacion;
import ar.edu.unq.chasqui.model.Pedido;
import ar.edu.unq.chasqui.model.Usuario;
import ar.edu.unq.chasqui.model.Variante;
import ar.edu.unq.chasqui.model.Vendedor;
import ar.edu.unq.chasqui.security.Encrypter;
import ar.edu.unq.chasqui.security.PasswordGenerator;
import ar.edu.unq.chasqui.service.rest.request.AgregarQuitarProductoAPedidoRequest;
import ar.edu.unq.chasqui.service.rest.request.ConfirmarPedidoRequest;
import ar.edu.unq.chasqui.service.rest.request.DireccionRequest;
import ar.edu.unq.chasqui.service.rest.request.EditarPerfilRequest;
import ar.edu.unq.chasqui.service.rest.request.SingUpRequest;
import ar.edu.unq.chasqui.services.interfaces.NotificacionService;
import ar.edu.unq.chasqui.services.interfaces.PedidoService;
import ar.edu.unq.chasqui.services.interfaces.ProductoService;
import ar.edu.unq.chasqui.services.interfaces.UsuarioService;

@Auditada
public class UsuarioServiceImpl implements UsuarioService{

	@Autowired
	private UsuarioDAO usuarioDAO;
	@Autowired
	private Encrypter encrypter;
	@Autowired
	private PasswordGenerator passwordGenerator;
	@Autowired
	private ProductoService productoService;
	@Autowired
	private MailService mailService;
	@Autowired
	PedidoService pedidoService;
	@Autowired
	NotificacionService notificacionService;
	
	public Usuario obtenerUsuarioPorID(Integer id) {
		return usuarioDAO.obtenerUsuarioPorID(id);
	}

	public Vendedor obtenerVendedorPorID(Integer id) {
		return usuarioDAO.obtenerVendedorPorID(id);
	}
	
	public void guardarUsuario(Usuario u) {
		usuarioDAO.guardarUsuario(u);
		
	}
	
	public void modificarPasswordUsuario(String email,String password){
		Usuario u = usuarioDAO.obtenerUsuarioPorEmail(email);
		u.setPassword(password);
		usuarioDAO.guardarUsuario(u);
	}
	

	
	/**
	 * Este metodo solo se debe usar para los tear down de los test
	 * @param obj
	 */
	@Transactional
	public <T> void deleteObject(T obj){
		usuarioDAO.deleteObject(obj);
	}
	

	public Usuario login(String username, String passwordHashed) throws Exception {
		Usuario usuario = usuarioDAO.obtenerUsuarioPorNombre(username);
		if (usuario != null ){
			String passwordUser = encrypter.decrypt(usuario.getPassword());
			if(passwordUser.equals(passwordHashed)){
				return usuario;				
			}
		}
		throw new Exception("Usuario o Password incorrectos!");
	}

	public void merguear(Vendedor usuario) {
		usuarioDAO.merge(usuario);
		
	}

	public Usuario obtenerUsuarioPorEmail(String email) {
		return usuarioDAO.obtenerUsuarioPorEmail(email);
	}
	
	public boolean existeUsuarioCon(String email){
		return usuarioDAO.existeUsuarioCon(email);
	}

	public Cliente loginCliente(String email, String password) throws Exception {
		Cliente c = (Cliente) obtenerUsuarioPorEmail(email);
		if(c != null){
			String passwordUser = encrypter.decrypt(c.getPassword());
			if(passwordUser.equals(password)){
				String token = passwordGenerator.generateRandomToken();
				c.setToken(token);
				usuarioDAO.guardarUsuario(c);
				return c;				
			}
		}
		throw new UsuarioExistenteException("No existe el usuario");
	}

	

	public Cliente crearCliente(SingUpRequest request) throws Exception {
		validarRequestCreacionDeUsuario(request);
		request.setPassword(encrypter.encrypt(request.getPassword()));
		Cliente c = new Cliente(request,passwordGenerator.generateRandomToken());
		usuarioDAO.guardarUsuario(c);
		return c;
	}

	@Override
	public void modificarUsuario(EditarPerfilRequest editRequest,String email) throws Exception {
		Cliente c = (Cliente) usuarioDAO.obtenerUsuarioPorEmail(email);
		if(c == null){
			throw new UsuarioExistenteException("No existe el usuario");
		}
		if(!StringUtil.isEmpty(editRequest.getPassword())){
			editRequest.setPassword(encrypter.encrypt(editRequest.getPassword()));
		}
		c.modificarCon(editRequest);
		usuarioDAO.guardarUsuario(c);
	}

	@Override
	public Direccion agregarDireccionAUsuarioCon(String mail, DireccionRequest request) {
		validarDireccionRequest(request);
		Cliente v = (Cliente) usuarioDAO.obtenerUsuarioPorEmail(mail);
		if(v == null){
			throw new UsuarioExistenteException("No existe el usuario con el mail: " + mail ); 
		}
		Direccion d = v.agregarDireccion(request);
		usuarioDAO.guardarUsuario(v);
		return d;
		
	}

	@Override
	public void inicializarListasDe(Vendedor usuarioLogueado) {
		usuarioDAO.inicializarListasDe(usuarioLogueado);
		
	}
	

	@Override
	public Cliente obtenerClienteConDireccion(String mail){
		Cliente c = usuarioDAO.obtenerClienteConDireccionPorEmail(mail);
		if(c == null){
			throw new UsuarioExistenteException("No se ha encontrado el usuario con el mail otorgado");
		}
		return c;
	}

	@Override
	public List<Direccion> obtenerDireccionesDeUsuarioCon(String mail) throws DireccionesInexistentes {
		Cliente c = usuarioDAO.obtenerClienteConDireccionPorEmail(mail);
		if(c == null){
			throw new UsuarioExistenteException("No se ha encontrado el usuario con el mail otorgado");
		}
		return c.getDireccionesAlternativas();
	}

	@Override
	public void editarDireccionDe(String mail,DireccionRequest request,Integer idDireccion) throws DireccionesInexistentes, UsuarioExistenteException {
		validarDireccionRequest(request);
		Cliente c = (Cliente)usuarioDAO.obtenerUsuarioPorEmail(mail);
		if(c == null){
			throw new UsuarioExistenteException("No existe el usuario");
		}
		if(idDireccion == null){
			throw new DireccionesInexistentes("No existe la direccion que se desea editar");
		}
		c.editarDireccionCon(request,idDireccion);
		usuarioDAO.guardarUsuario(c);
	}

	@Override
	public void eliminarDireccionDe(String mail, Integer idDireccion)
			throws DireccionesInexistentes, UsuarioExistenteException {
		Cliente c = (Cliente) usuarioDAO.obtenerUsuarioPorEmail(mail);
		if(c == null){
			throw new UsuarioExistenteException("No existe el usuario");
		}
		c.eliminarDireccion(idDireccion);
		usuarioDAO.guardarUsuario(c);
		
	}
	

	@Override
	public Pedido obtenerPedidoActualDe(String mail,Integer idVendedor) throws PedidoInexistenteException {
		return ( (Cliente) usuarioDAO.obtenerUsuarioPorEmail(mail)).obtenerPedidoActualDe(idVendedor);
	}
	
	
	@Override
	public List<Pedido> obtenerPedidosVigentesDe(String mail) {
		return ( (Cliente) usuarioDAO.obtenerUsuarioPorEmail(mail)).obtenerPedidosVigentes();
	}

	@Override
	public void crearPedidoPara(String mail,Integer idVendedor) {
		Cliente c = usuarioDAO.obtenerClienteConPedido(mail);
		Vendedor v = usuarioDAO.obtenerVendedorPorID(idVendedor);
		validarVendedorParaCreacionDePedido( c, v);
		DateTime d = new DateTime();
		if(v.getFechaCierrePedido().isBefore(d)){
			v.setFechaCierrePedido(v.getFechaCierrePedido().plusMonths(1));
			usuarioDAO.guardarUsuario(v);
		}
		
		Pedido p = new Pedido(v,c);
		c.agregarPedido(p);
		usuarioDAO.guardarUsuario(c);
		
	}

	private void validarVendedorParaCreacionDePedido(Cliente c, Vendedor v) {
		if(c == null){
			throw new UsuarioExistenteException("No se ha encontrado el usuario con el mail otorgado");
		}
		if(v == null || v.getIsRoot() ){
			throw new UsuarioExistenteException("Vendedor Inexistente");
		}
		
		if(v.getFechaCierrePedido() == null){
			throw new UsuarioExistenteException("El Vendedor al que se le desea crear un pedido, aún no ha definido la fecha de cierre. No es posible crear el pedido");
		}
		
		if(v.getMontoMinimoPedido() == null || v.getMontoMinimoPedido() < 0){
			throw new UsuarioExistenteException("El Vendedor al que se le desea crear un pedido, aún no ha definido el monto minimo de compra. No es posible crear el pedido");
		}
		
		if(c.contienePedidoVigenteParaVendedor(v.getId())){
			throw new PedidoVigenteException("El usuario: "+ c.getNickName() +" ya posee un pedido vigente para el vendedor brindado");
		}
		
		if(c.obtenerDireccionPredeterminada() == null){
			throw new PedidoVigenteException("El usuario: "+ c.getNickName() +" no posee una direccion predeterminada");
		}
	}

	@Override
	public synchronized void agregarPedidoA(AgregarQuitarProductoAPedidoRequest request, String email) {
		validarRequest(request);
		Cliente c = usuarioDAO.obtenerClienteConPedido(email);
		Variante v = productoService.obtenerVariantePor(request.getIdVariante());
		validar(v,c,request);
		c.agregarProductoAPedido(v,request.getIdPedido(),request.getCantidad());
		usuarioDAO.guardarUsuario(c);
		v.reservarCantidad(request.getCantidad());
		productoService.modificarVariante(v);		
	}

	private void validar(Variante v, Cliente c,AgregarQuitarProductoAPedidoRequest request) {		
		validacionesGenerales(v,c,request);		
		if(!v.tieneStockParaReservar(request.getCantidad())){
			throw new ProductoInexsistenteException("El producto no posee más Stock");
		}	
	}
	
	private void validarRequest(ConfirmarPedidoRequest request){
		validarRequest(request.getIdPedido());
//		validarRequest(request.getIdDireccion());
		
	}
	
	private void validarRequest(Integer idPedido){
		if(idPedido == null || idPedido < 0){
		throw new RequestIncorrectoException("La cantidad debe ser mayor a 0");
		}
	}
	
	private void validacionesGenerales(Variante v, Cliente c, AgregarQuitarProductoAPedidoRequest request){
		if(v == null){
			throw new ProductoInexsistenteException("No existe el producto con ID: "+ request.getIdVariante());
		}
		if(!c.contienePedidoVigente(request.getIdPedido())){
			throw new PedidoVigenteException("El usuario no posee el pedido con ID:" + request.getIdPedido()+" o el mismo no se encuentra vigente");
		}
		
		if(!c.varianteCorrespondeConPedido(v.getIdVendedor(),request.getIdPedido())){
			throw new RequestIncorrectoException("El producto no corresponde con el vendedor al que se le hizo el pedido con ID: "+ request.getIdPedido());
		}		
	}

	@Override
	public synchronized void eliminarProductoDePedido(AgregarQuitarProductoAPedidoRequest request, String email) {
		validarRequest(request);
		Cliente c = usuarioDAO.obtenerClienteConPedido(email);
		Variante v = productoService.obtenerVariantePor(request.getIdVariante());
		validarParaEliminar(v,c,request);
		c.eliminarProductoEnPedido(request.getIdVariante(), v.getPrecio(),request.getIdPedido(),request.getCantidad());
		v.eliminarReserva(request.getCantidad());
		usuarioDAO.guardarUsuario(c);
		productoService.modificarVariante(v);
	}

	private void validarParaEliminar(Variante v, Cliente c, AgregarQuitarProductoAPedidoRequest request) {
		validacionesGenerales(v, c, request);
		if(!c.contieneProductoEnPedido(v,request.getIdPedido())){
			throw new ProductoInexsistenteException("El usuario no tiene el producto con ID "+ request.getIdVariante()+" en el pedido");
		}
		if(!c.contieneCantidadDeProductoEnPedido(v,request.getIdPedido(),request.getCantidad())){
			throw new ProductoInexsistenteException("No se puede quitar mas cantidad de un producto de la que el usuario posee en su pedido");
		}	
	}

	@Override
	public synchronized void eliminarPedidoPara(String email, Integer idPedido) {
		validarRequest(idPedido);
		Cliente c = usuarioDAO.obtenerClienteConPedido(email);
		validarEliminacionDePedidoPara(c,idPedido);
		Pedido p = c.eliminarPedido(idPedido);
		productoService.eliminarReservasDe(p);
		usuarioDAO.guardarUsuario(c);
	}

	private void validarEliminacionDePedidoPara(Cliente c, Integer idPedido) {
		if(!c.contienePedidoVigente(idPedido)){
			throw new PedidoVigenteException("El usuario no posee el pedido con ID:" + idPedido+" o el mismo no se encuentra vigente");
		}
		
	}

	@Override
	public void eliminarUsuario(Vendedor u) {
		usuarioDAO.eliminarUsuario(u);
		
	}

	@Override
	public List<Notificacion> obtenerNotificacionesDe(String mail, Integer pagina) {
		return usuarioDAO.obtenerNotificacionesDe(mail,pagina);
	}

	@Override
	public void enviarInvitacionRequest(String origen,String destino) throws Exception {
		
		Cliente clienteOrigen = (Cliente) usuarioDAO.obtenerUsuarioPorEmail(origen);
		Cliente clienteDestino = (Cliente) usuarioDAO.obtenerUsuarioPorEmail(destino);
		if(clienteDestino != null){
			// enviar Notificacion de creacion de grupo
		}else{
			mailService.enviarEmailDeInvitacionChasqui(clienteOrigen,destino);
		}
		
		
	}

	@Override
	public synchronized void confirmarPedido(String email, ConfirmarPedidoRequest request) throws PedidoInexistenteException {
		validarRequest(request);
		Cliente c = (Cliente) usuarioDAO.obtenerClienteConPedidoEHistorial(email);
		validarConfirmacionDePedidoPara(c,request);
		Pedido p = c.encontrarPedidoConId(request.getIdPedido());
		Vendedor v = (Vendedor) usuarioDAO.obtenerVendedorPorID(p.getIdVendedor());
		v.descontarStockYReserva(p);
		c.confirmarPedido(request);
		mailService.enviarEmailConfirmacionPedido(v.getEmail(),email,p);
		usuarioDAO.guardarUsuario(c);
		usuarioDAO.guardarUsuario(v);
//		Notificacion n = new Notificacion(v.getNombre(), email, "El pedido con id "+p.getId()+" se generó correctamente.", Constantes.ESTADO_NOTIFICACION_NO_LEIDA);
//		notificacionService.guardar(n, "NULL");
	}

	private void validarConfirmacionDePedidoPara(Cliente c,ConfirmarPedidoRequest request) throws PedidoInexistenteException {
		if(!c.contienePedidoVigente(request.getIdPedido())){
			throw new PedidoInexistenteException("El usuario: "+c.getNickName()+" no posee un pedido vigente con el ID otorgado");
		}		
		
		if(!c.contieneDireccion(request.getIdDireccion())){
			throw new PedidoInexistenteException("El usuario: "+c.getNickName()+" no posee una direccion con el ID otorgado");
		}
	}
	
	private void validarRequest(AgregarQuitarProductoAPedidoRequest request) {
		if(request.getIdPedido() == null){
			throw new RequestIncorrectoException("El id De pedido no debe ser null");
		}
		if(request.getIdVariante() == null){
			throw new RequestIncorrectoException("El id de variante no debe ser null");
		}
		if(request.getCantidad() == null || request.getCantidad() <= 0){
			throw new RequestIncorrectoException("La cantidad debe ser mayo  debe ser mayor a 0");
		}
	}


	
	private void validarRequestCreacionDeUsuario(SingUpRequest request) throws RequestIncorrectoException {
		if(StringUtil.isEmpty(request.getEmail())){
			throw new RequestIncorrectoException("Debe completar todos los campos");
		}
		
		if(!EmailValidator.getInstance().isValid(request.getEmail())){
			throw new RequestIncorrectoException("Email invalido");
		}
		
		if(StringUtil.isEmpty(request.getApellido())){
			throw new RequestIncorrectoException("Debe completar todos los campos");
		}
		
		if(StringUtil.isEmpty(request.getNickName())){
			throw new RequestIncorrectoException("Debe completar todos los campos");
		}
		
		if(StringUtil.isEmpty(request.getNombre())){
			throw new RequestIncorrectoException("Debe completar todos los campos");
		}
		if(StringUtil.isEmpty(request.getPassword())){
			throw new RequestIncorrectoException("Debe completar todos los campos");
		}
//		if(request.getDireccion() == null){
//			throw new RequestIncorrectoException("Debe completar todos los campos");
//		}
		
		if(request.getTelefonoFijo() == null){
			throw new RequestIncorrectoException("Debe completar todos los campos");
		}
		
		
	//	validarDireccion(request.getDireccion());
		
		if(this.existeUsuarioCon(request.getEmail())){
			throw new UsuarioExistenteException("El email ya se encuentra en uso");
		}
	}
	
	private void validarDireccionRequest(DireccionRequest request) {
		if(StringUtils.isEmpty(request.getAlias())){
			throw new RequestIncorrectoException();
		}
		if(StringUtils.isEmpty(request.getCalle())){
			throw new RequestIncorrectoException();
		}
		if(StringUtils.isEmpty(request.getCodigoPostal())){
			throw new RequestIncorrectoException();
		}
		if(request.getPredeterminada() == null){
			throw new RequestIncorrectoException();
		}
		if(StringUtils.isEmpty(request.getLocalidad())){
			throw new RequestIncorrectoException();
		}
		if(request.getAltura() == null || request.getAltura() < 0){
			throw new RequestIncorrectoException();
		}
		
		
	}

	@Override
	public void agregarIDDeDispositivo(String mail, String dispositivo) {
		Cliente c = (Cliente) this.obtenerUsuarioPorEmail(mail);
		if(c != null){
			c.setIdDispositivo(dispositivo);
			this.guardarUsuario(c);
		}
		
	}

	@Override
	public List<Notificacion> obtenerNotificacionesNoLeidas(String mail) {
		return usuarioDAO.obtenerNotificacionNoLeidas(mail);
	}

	@Override
	public Integer obtenerTotalNotificacionesDe(String mail) {
		return usuarioDAO.obtenerTotalNotificacionesDe(mail);
	}

	@Override
	public void leerNotificacion(Integer id) {
		Notificacion n = usuarioDAO.obtenerNotificacion(id);
		if(n != null){
			n.setEstado("Leído");
			usuarioDAO.guardar(n);
		}
		
	}

	
	// private void validarDireccion(DireccionRequest direccion) {
//		if(StringUtils.isEmpty(direccion.getCalle())){
//			throw new RequestIncorrectoException("Debe completar todos los campos");
//		}
//		
//		if(StringUtils.isEmpty(direccion.getLocalidad())){
//			throw new RequestIncorrectoException("Debe completar todos los campos");
//		}
//		
//		if(StringUtils.isEmpty(direccion.getCodigoPostal())){
//			throw new RequestIncorrectoException("Debe completar todos los campos");
//		}
//		
//		if(direccion.getAltura() == null){
//			throw new RequestIncorrectoException("Debe completar todos los campos");
//		}
//		if(direccion.getLongitud() == null){
//			throw new RequestIncorrectoException("Debe completar todos los campos");
//		}
//		if(direccion.getLatitud() == null){
//			throw new RequestIncorrectoException("Debe completar todos los campos");
//		}
//	}

	

	

}
