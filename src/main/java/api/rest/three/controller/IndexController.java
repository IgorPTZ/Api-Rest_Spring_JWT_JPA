package api.rest.three.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import api.rest.three.model.Usuario;
import api.rest.three.model.UsuarioDTO;
import api.rest.three.repository.UsuarioRepository;

/* Arquitetura REST */

@RestController
@RequestMapping(value = "/usuario")
public class IndexController {
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	/* Serviço RESTful */
	
	/* Versionamento de API */
	@GetMapping(value = "v1/{id}", produces = "application/json")
	
	/* O CacheEvict  remove todos os caches antigos que nao sao mais atualizados e utilizados */
	@CacheEvict(value="cache-obter-usuario-v1", allEntries = true)
	
	/* O CachePut identifica atualizacoes e adiciona ao cache */
	@CachePut("cache-obter-usuario-v1")
	public ResponseEntity<UsuarioDTO> obterUsuarioV1(@PathVariable (value = "id") Long id) {
		
		Optional<Usuario> usuario = usuarioRepository.findById(id);
		
		System.out.println("Old version, for most clients");

		return new ResponseEntity<UsuarioDTO>(new UsuarioDTO(usuario.get()), HttpStatus.OK);
	}
	
	/* Versionamento de API */
	@GetMapping(value = "v2/{id}", produces = "application/json")
	@CacheEvict(value="cache-obter-usuario-v2", allEntries = true)
	@CachePut("cache-obter-usuario-v2")
	public ResponseEntity<UsuarioDTO> obterUsuarioV2(@PathVariable (value = "id") Long id) {
		
		Optional<Usuario> usuario = usuarioRepository.findById(id);
		
		System.out.println("New version, only some clients");

		return new ResponseEntity<UsuarioDTO>(new UsuarioDTO(usuario.get()), HttpStatus.OK);
	}
	
	
	@GetMapping(value="/", produces = "application/json")
	@CacheEvict(value="cache-obter-usuario", allEntries = true)
	@CachePut("cache-obter-usuario")
	public ResponseEntity<List<Usuario>> obterUsuarios() throws InterruptedException {
		
		List<Usuario> usuarios = (List<Usuario>) usuarioRepository.findAll();
		
		return new ResponseEntity<List<Usuario>>(usuarios, HttpStatus.OK);
	}
	
	
	@PostMapping(value = "/", produces = "application/json")
	public ResponseEntity<Usuario> cadastrarUsuario(@RequestBody Usuario usuario) throws IOException {
		
		for(int i = 0; i < usuario.getTelefones().size(); i ++) {
			usuario.getTelefones().get(i).setUsuario(usuario);
		}
		
		/* Consumindo API do ViaCEP - Inicio */
		URL url = new URL("https://viacep.com.br/ws/" + usuario.getCep() + "/json/");
		
		URLConnection connection = url.openConnection();
		
		InputStream inputStream = connection.getInputStream();
		
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		
		String auxiliar = "";
		
		StringBuilder json = new StringBuilder();
		
		while((auxiliar = bufferedReader.readLine()) != null) {
			
			json.append(auxiliar);
		}		
		
		Usuario usuarioAuxiliar = new Gson().fromJson(json.toString(), Usuario.class);
		/* Consumindo API do ViaCEP - Fim */
			
		String senhaCriptografada = new BCryptPasswordEncoder().encode(usuario.getSenha());
		
		usuario.setSenha(senhaCriptografada);
		
		usuario.setCep(usuarioAuxiliar.getCep());
		
		usuario.setLogradouro(usuarioAuxiliar.getLogradouro());
		
		usuario.setComplemento(usuarioAuxiliar.getComplemento());
		
		usuario.setBairro(usuarioAuxiliar.getBairro());
		
		usuario.setLocalidade(usuarioAuxiliar.getLocalidade());
		
		usuario.setUf(usuarioAuxiliar.getUf());
		
		Usuario usuarioSalvo = usuarioRepository.save(usuario);
		
		return new ResponseEntity<Usuario>(usuarioSalvo, HttpStatus.OK);
	}
	
	
	@PutMapping(value = "/", produces = "application/json")
	public ResponseEntity<Usuario> atualizarUsuario(@RequestBody Usuario usuario) {
		
		for(int i = 0; i < usuario.getTelefones().size(); i ++) {
			usuario.getTelefones().get(i).setUsuario(usuario);
		}
		
		Usuario aux = usuarioRepository.findUserByLogin(usuario.getLogin());
		
		/* Caso a senha enviada seja diferente a que se encontra no banco de dados, havera 
		 * uma atualizacao de senha, e a nova senha sera criptografada e salva no banco de dados */
		if(!aux.getSenha().equals(usuario.getSenha())) {
			
			String senhaCriptografada = new BCryptPasswordEncoder().encode(usuario.getSenha());
			usuario.setSenha(senhaCriptografada);
		}
		
		Usuario usuarioAtualizado = usuarioRepository.save(usuario);
		
		return new ResponseEntity<Usuario>(usuarioAtualizado, HttpStatus.OK);
	}
	
	
	@DeleteMapping(value = "/{id}", produces = "application/text")
	public String excluirUsuario(@PathVariable("id") Long id) {
		
		usuarioRepository.deleteById(id);
		
		return "Usuario excluido com sucesso!";
	}
}
