package control;

import model.Avaliacao;
import model.ManagerEntities;
import model.User;
import model.AcessDataBase.ArquivoPergunta;
import view.ContextView;
import java.text.Normalizer;
import model.Pergunta;
import model.Resposta;

import  java.util.ArrayList;



public class Controller {
    private ContextView context;
    private ManagerEntities managerModel;    

    public Controller(ContextView context, ManagerEntities managerEntities){
        this.context = context;
        this.managerModel = managerEntities;
        this.context.setControl(this);
        this.managerModel.setControl(this);
        context.start();
    }

    private boolean checkValidEmail(String email){
        boolean result = false;
        email = email.strip();
        if(email.length() > 0){
            result = email.contains("@") && email.contains(".");
        }

        return result;
    }

    public static String normalizeResponse(String resposta){
        return Normalizer.normalize(resposta.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    public static int hashRespostaSecreta(String resposta){
        return Math.abs(normalizeResponse(resposta).hashCode());
    }

    public void addUser(User user) throws Exception{
        if(checkValidEmail(user.getEmail())){
            //user.setPerguntaSecreta(normalizeResponse(user.getPerguntaSecreta()));
            //user.setHashRespostaSecreta(user.getHashRespostaSecreta());
            managerModel.getArqUsers().create(user);
        }else{
            throw new IllegalArgumentException("Erro : Email Inválido!");
        }
    }

    public boolean updateUser(User newUser){
        if(newUser == null){
            return false;
        }

        try {
            if(!checkValidEmail(newUser.getEmail())){
                throw new IllegalArgumentException("Erro: Email invalido");
            }

          //  newUser.setPerguntaSecreta(newUser.getPerguntaSecreta());
            return managerModel.getArqUsers().update(newUser);
        } catch (Exception e) {
           context.reportFail(e,true);
           return false; 
        }
    }

    public boolean updateSenhaUser(String email, String newHashSenha){
        try {
            User user = managerModel.getArqUsers().read(email);
            user.setHashSenha(newHashSenha); 
            return managerModel.getArqUsers().update(user);
        } catch (Exception e) {
           context.reportFail(e,true);
           return false; 
        }
    }

    public boolean deleteUser(int id) throws Exception{
        // Cascata: os filhos saem antes do pai, para não sobrar órfão
        for(Avaliacao a : managerModel.getArqAvalaiacao().readByUser(id)){
            desfazerAvaliacao(a);
        }

        for(Resposta r : managerModel.getArqResposta().readByUser(id)){
            apagarResposta(r.getId());
        }
      
        for(Pergunta p : managerModel.getArqPergunta().readByUser(id)){
            for(Resposta r : managerModel.getArqResposta().readTodasByPergunta(p.getId())){
                apagarResposta(r.getId());
            }
            managerModel.getArqPergunta().delete(p.getId());
        }

        return managerModel.getArqUsers().delete(id);
    }

    // apaga a resposta mas  antes dela as avaliações 
    private void apagarResposta(int idResposta) throws Exception{

        for(Avaliacao a : managerModel.getArqAvalaiacao().readByResposta(idResposta)){
            managerModel.getArqAvalaiacao().delete(a.getId());
        }

        managerModel.getArqResposta().delete(idResposta);
    }
    
    public boolean findUser(String email) {
        try {
            return managerModel.getArqUsers().read(email) != null ? true : false;
        } catch (Exception e) {
            context.reportFail(e,true);
            return false;
        }
    }

    public User readUser(String email, String hashPassword) throws Exception{
        User user = managerModel.getArqUsers().read(email);
        if(user != null){
            if(!hashPassword.equalsIgnoreCase(user.getHashSenha())){
                user = null;
            }
        }else{
            throw new IllegalAccessException("Error Senha ou Email inválidos!");
        }
      
        return user;

    }

    public String readShowUser(int id) throws Exception{
        User user = managerModel.getArqUsers().read(id);
        if(user == null){
            throw new IllegalAccessException("Usuário não encontrado!");
        }
      
        return user.getEmail() + ";" + user.getNome();

    }

    public String [] recoveryQuestion(String email) throws Exception{
        User user = managerModel.getArqUsers().read(email);
        if(user != null){
            String vals [] = new String [2];
            vals[0] = user.getPerguntaSecreta();
            vals[1] = String.valueOf(user.getHashRespostaSecreta());
            return vals;
        }else{
            throw new IllegalAccessException("Ocorreu um erro durante a recuperação!");
        }
    }

    private int idUserLogado() throws Exception{
        return  SingletonUser.getInstanceUser().getId();
    }

    public ArrayList<Pergunta> listarMinhasPerguntas() throws Exception{
        return   managerModel.getArqPergunta().readByUser(idUserLogado());
    }

    public int addPergunta(String text, String keyWord) throws Exception{
        int idUsuario = idUserLogado();
        text = text.strip();
        keyWord = keyWord.strip();
        if(text.isEmpty() ||   keyWord.isEmpty()){
            throw new IllegalArgumentException("Conteudo da Pergunta ou das Palavras Chave vazio");
        }else if(text.length() > 4000){
            throw new  IllegalArgumentException("Pergunta excede o numero de caracteres");
        }else if(keyWord.length() > 1000){
            throw new  IllegalArgumentException("Palavras Chave excedem o numero de caracteres");
        }else if(managerModel.getArqUsers().read(idUsuario) == null){
            throw new IllegalArgumentException("Usuario não encontrado");
        }else{
            Pergunta p = new Pergunta(idUsuario, text, keyWord);
            return  managerModel.getArqPergunta().create(p);
        }
    }

    public boolean updatePergunta(Pergunta p, String newText, String newWords) throws Exception{
        boolean resp;
        if(p.getIdUsuario() != idUserLogado()){
            throw new IllegalAccessException ("Usuario não correspondente");
        }else if(!p.isAtiva()){
            throw new IllegalAccessException ("Pergunta arquivada não pode ser alterada");
        }

        newText = newText.strip();
        newWords = newWords.strip();
        if(newText.isEmpty() && newWords.isEmpty()){
            resp =  false;
        }else if(newText.length() > 4000){
            throw new IllegalArgumentException("Pergunta excede o numero de caracteres");
        }else if(newWords.length() > 1000){
            throw new IllegalArgumentException("Palavras Chave excedem o numero de caracteres");
        }else{

            if(!newText.isEmpty()){
                p.setPergunta(newText);
            }
            if(!newWords.isEmpty()){
                p.setPalavrasChave(newWords);
            }
            p.setAlteracao(System.currentTimeMillis());
            resp = managerModel.getArqPergunta().update(p);
        }
        return resp;
    }

    public boolean arquivarPergunta(Pergunta p) throws Exception{
        if(p.getIdUsuario() != idUserLogado()){
            throw new IllegalAccessException ("Usuario não correspondente");
        }else if(!p.isAtiva()){
            throw new IllegalAccessException ("Pergunta já arquivada");
        }
        p.setAlteracao(System.currentTimeMillis());
        p.arquivar();
        return managerModel.getArqPergunta().update(p);
    }

    public Pergunta readPergunta(int id) throws Exception{
        return managerModel.getArqPergunta().readPergunta(id);
    }

    public ArrayList<Pergunta> readPerguntasByWords(String words) throws Exception{
        return managerModel.getArqPergunta().readPerguntasByWords(words);
    }

    public static final int PERGUNTAS_POR_PAGINA = 5;

    public ArrayList<Pergunta> listarPerguntasRecentes(int pagina) throws Exception{

        return managerModel.getArqPergunta().readRecentes(pagina, PERGUNTAS_POR_PAGINA);
    }

    public int totalPaginasRecentes() throws Exception{
        int total = managerModel.getArqPergunta().contarRecentes();

        return (total + PERGUNTAS_POR_PAGINA - 1) / PERGUNTAS_POR_PAGINA; 
    }

    public String nomeUsuario(int id) throws Exception{

        User user = managerModel.getArqUsers().read(id);

        if(user == null){
            throw new IllegalAccessException("Usuário não encontrado!");
        }

        return user.getNome();
    }
    
//////////RESPOSTAS////////

    public ArrayList<Resposta> readRespostasByUser(int idUsuario) throws Exception {
        return managerModel.getArqResposta().readByUser(idUsuario);
    }

    public ArrayList<Resposta> readRespostasByPergunta(int idPergunta) throws Exception {
        ArrayList<Resposta> respostas = managerModel.getArqResposta().readByPergunta(idPergunta);

        respostas.sort((a, b) -> b.getNota() - a.getNota()); // 1 a maior

        return respostas;
    }

    public Resposta readResposta(int id) throws Exception {
        return managerModel.getArqResposta().read(id);
    }


    public int addResposta(int idPergunta, String texto) throws Exception {
        if (texto == null || texto.strip().isEmpty()){
            throw new Exception("A resposta não pode ser vazia");
        } 

        texto = texto.strip();
        if (texto.length() > 4000){
            throw new Exception("A resposta excede 4000 caracteres");
        }
            
        int idUsuario = idUserLogado();
        if (managerModel.getArqUsers().read(idUsuario) == null){
            throw new Exception("Usuario nao encontrado");
        }
            
        Pergunta p = managerModel.getArqPergunta().read(idPergunta);
        if (p == null){
            throw new Exception("Pergunta nao encontrada");
        }
            
        if (!p.isAtiva()){
            throw new Exception("Esta pergunta esta arquivada e nao aceita respostas");
        }
            
        Resposta r = new Resposta(-1, idPergunta, idUsuario, System.currentTimeMillis(), texto, (short) 0, (short) 0, true);
        return managerModel.getArqResposta().create(r);
    }

    public boolean arquivarResposta(Resposta r) throws Exception{

        int idLogado = idUserLogado();

        Pergunta p = managerModel.getArqPergunta().read(r.getIdPergunta());

        boolean autorDaResposta = r.getIdUsuario() == idLogado;
        boolean autorDaPergunta = p != null && p.getIdUsuario() == idLogado;

        if(!autorDaResposta && !autorDaPergunta){
            throw new IllegalAccessException("Só o autor da resposta ou da pergunta pode arquivá-la");
        }else if(!r.isAtiva()){
            throw new IllegalAccessException("Resposta já arquivada");
        }

        r.arquivar();

        return managerModel.getArqResposta().update(r);
    }


    public boolean incrementAvaliacao(int idR) throws Exception{       
        return managerModel.getArqResposta().incrementAvaliacao(idR);
    }

    
    public boolean incrementNota(int idR) throws Exception{
        return managerModel.getArqResposta().incrementNota(idR);
    }

    
    public boolean decrementNota(int idR) throws Exception{
        return managerModel.getArqResposta().decrementNota(idR);
    }
    
    //////////AVALIACAO////////

    // addAvaliação

    public void addAvaliacao(Avaliacao av){
        try{
            managerModel.getArqAvalaiacao().create(av);
        }catch(Exception e){
            context.reportFail(e,false);
        }
    }

    // existAvalaicaoUserResponse
    public boolean existAvalaicaoUserResponse(int idUser, int idResponse) throws Exception{
        boolean exist = false;
        for(Avaliacao a : managerModel.getArqAvalaiacao().readByUser(idUser)){
            if(a.getIdResposta() == idResponse){
                exist = true;
                break;
            }
        }

        return exist;
    }

    public void avaliarResposta(Resposta r, boolean positiva) throws Exception{

        int idLogado = idUserLogado();

        if(r.getIdUsuario() == idLogado){
            throw new IllegalAccessException("Você não pode avaliar a própria resposta");
        }else if(!r.isAtiva()){
            throw new IllegalAccessException("Resposta arquivada não pode ser avaliada");
        }else if(existAvalaicaoUserResponse(idLogado, r.getId())){
            throw new IllegalAccessException("Você já avaliou esta resposta");
        }

        managerModel.getArqAvalaiacao().create(new Avaliacao(-1, idLogado, r.getId(), positiva));

        managerModel.getArqResposta().incrementAvaliacao(r.getId());

        if(positiva){
            managerModel.getArqResposta().incrementNota(r.getId());
        }else{
            managerModel.getArqResposta().decrementNota(r.getId());
        }
    }

    public ArrayList<Avaliacao> listarMinhasAvaliacoes() throws Exception{
        return managerModel.getArqAvalaiacao().readByUser(idUserLogado());
    }

    public boolean cancelarAvaliacao(Avaliacao a) throws Exception{
        if(a.getIdUsuario() != idUserLogado()){
            throw new IllegalAccessException("Esta avaliação não é sua");
        }
        return desfazerAvaliacao(a);
    }

    private boolean desfazerAvaliacao(Avaliacao a) throws Exception{

        if(a.getItSelfAvalaiacao()){
            managerModel.getArqResposta().decrementNota(a.getIdResposta());
        }else{
            managerModel.getArqResposta().incrementNota(a.getIdResposta());
        }

        managerModel.getArqResposta().decrementAvaliacao(a.getIdResposta());

        return managerModel.getArqAvalaiacao().delete(a.getId());
    }
    
}
