package model.AcessDataBase;

import model.Pergunta;

import java.text.Normalizer;
import  java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;


import Formatter.FormatterString;

public class ArquivoPergunta extends Arquivo<Pergunta>{
    ArvoreBMais<ParIdId> relUsuarioPergunta;
    ArvoreBMais<ParDataId> indiceData;
    ListaInvertida invertedList;

    private static final HashSet<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "o", "as", "os", "um", "uma", "uns", "umas", "dois", "duas", "tres",
        "de", "da", "do", "das", "dos", "em", "na", "no", "nas", "nos", "num", "numa",
        "ao", "aos", "por", "pelo", "pela", "pelos", "pelas", "para", "pra", "pro",
        "com", "sem", "sob", "sobre", "entre", "ate", "apos", "contra", "desde",
        "e", "ou", "nem", "mas", "que", "se", "como", "porque", "quando", "onde",
        "qual", "quais", "isso", "isto", "esse", "essa", "este", "esta", "aquele", "aquela",
        "eu", "voce", "ele", "ela", "seu", "sua", "meu", "minha",
        "nao", "sim", "mais", "menos", "muito", "ja", "tambem", "so",
        "ser", "sao", "foi", "ha", "tem",
        "duvida", "ajuda", "pergunta", "alguem"
    ));

    public ArquivoPergunta () throws Exception{
        super("perguntas", Pergunta.class.getConstructor());
        relUsuarioPergunta = new ArvoreBMais<>(ParIdId.class.getConstructor(),
         5, "./dados/perguntas/relUsuarioPergunta.db");
        
        indiceData = new ArvoreBMais<>(ParDataId.class.getConstructor(),
         5, "./dados/perguntas/indiceData.db");

        invertedList = new ListaInvertida(30, "./dados/perguntas/DictionaryQuestions.db", "./dados/perguntas/InvertedQuestions.db");
        if(indiceData.empty()){
            reconstruirIndiceData();
        }
        if(invertedList.numeroEntidades() == 0){
            reconstruirIndiceInvertido();
        }
    }

    private static ArrayList<String> extrairTermos(String texto){
        ArrayList<String> termos = new ArrayList<>();
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();

        for(String termo : normalizado.split("[^a-z0-9+#]+")){
            if(!termo.isEmpty() && !STOP_WORDS.contains(termo)){
                termos.add(termo);
            }
        }
        return termos;

    }

    private void createRefInvert(Pergunta p) throws Exception{
        ArrayList<String> termos = extrairTermos(p.getPalavrasChave());
        float tf = 1 / (float) termos.size();

        for(String termo : termos){
            ElementoLista e = invertedList.read(termo, p.getId());
            if(e == null){
                invertedList.create(termo, new ElementoLista(p.getId(), tf));
            }else{
                invertedList.update(termo, new ElementoLista(p.getId(), e.getFrequencia() + tf));
            }
        }
    }

    public void removeAllKeyWords(Pergunta p)throws Exception{
        for(String termo : extrairTermos(p.getPalavrasChave())){
            invertedList.delete(termo, p.getId());
        }
    }

    private void reconstruirIndiceInvertido() throws Exception{
        for(Pergunta p : super.readAll()){
            invertedList.incrementaEntidades();
            createRefInvert(p);
        }
    }


    private void reconstruirIndiceData() throws Exception{
        for(Pergunta p : super.readAll()){
            if(p.isAtiva()){
                indiceData.create(new ParDataId(p.getCriacao(), p.getId()));
            }
        }
    }

    @Override
    public int create(Pergunta p) throws Exception{
        int id = super.create(p);

        relUsuarioPergunta.create(new ParIdId(p.getIdUsuario(), id));

        indiceData.create(new ParDataId(p.getCriacao(), id));
        p.setId(id);
        invertedList.incrementaEntidades();
        createRefInvert(p);

        return id;
    }

    public Pergunta readPergunta(int id) throws Exception{
        Pergunta p = super.read(id);
        return p;
    }

    public ArrayList<Pergunta> readByUser(int idUsuario) throws Exception{
        ArrayList<Pergunta> perguntas = new ArrayList<>();
        ArrayList<ParIdId> pares = relUsuarioPergunta.read(new ParIdId(idUsuario, -1));
        for(ParIdId par : pares){
            Pergunta p = super.read(par.getId2());
            if(p != null){
                perguntas.add(p);
            }
        }
        return perguntas;
    }

    //Pagina vai de 0 ate PorPagina
    public ArrayList<Pergunta> readRecentes(int pagina, int porPagina) throws Exception{
        ArrayList<Pergunta> perguntas = new ArrayList<>();
        ArrayList<ParDataId> pares = indiceData.read(null); //todos crescente

        int inicio = pagina * porPagina;
        int fim = Math.min(inicio + porPagina, pares.size());

        for(int i = inicio; i < fim; i++){
            Pergunta p = super.read(pares.get(i).getId());
            if(p != null){
                perguntas.add(p);
            }
        }
        return perguntas;
    }

    public int contarRecentes() throws Exception{
        return indiceData.read(null).size();
    }


    public ArrayList<Pergunta> readPerguntasByWords(String consulta) throws Exception{
        ArrayList<Pergunta> resultado = new ArrayList<>();
        
        LinkedHashSet<String> termos = new LinkedHashSet<>(extrairTermos(consulta));

        int total = invertedList.numeroEntidades();
        if (termos.isEmpty() || total <= 0){
            return resultado;
        }

        //calculo tfxidf
        double idf;
        HashMap<Integer, Double> scores = new HashMap<>();
        for(String termo : termos){
            ElementoLista[] lista = invertedList.read(termo);
            if(lista.length > 0){
                idf = Math.log10((double) total / lista.length);

                for(int i = 0; i < lista.length; i++){
                    int id = lista[i].getId();
                    double valor = lista[i].getFrequencia() * idf;
                    
                    if(scores.get(id) == null){
                        scores.put(id, valor);
                    }else{
                        scores.put(id, scores.get(id) + valor);
                    }
                }
            }
        }
        
        //ordancao !!!IA UTILIZADA NESTE TRECHO!!!
        ArrayList<Integer> ids = new ArrayList<>(scores.keySet());
        ids.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));

        for (int i = 0; i < ids.size(); i++) {
            Pergunta p = super.read(ids.get(i));
            if (p != null && p.isAtiva()) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    @Override
    public boolean update(Pergunta p) throws Exception{
        // a versão gravada ainda tem as palavras-chave antigas; o p já chega com as novas
        Pergunta antiga = super.read(p.getId());
        boolean ok = super.update(p);
        if(ok){
            removeAllKeyWords(antiga);
            createRefInvert(p);
        }
        if(ok && !p.isAtiva()){
            // pergunta arquivada nao fica publica
            indiceData.delete(new ParDataId(p.getCriacao(), p.getId()));
        }
        return ok;
    }



    @Override
    public boolean delete(int id) throws Exception{
        Pergunta p = super.read(id);
        
        if(p == null){
            return false;
        }
        invertedList.decrementaEntidades();
        removeAllKeyWords(p);
        relUsuarioPergunta.delete(new ParIdId(p.getIdUsuario(), id));
        indiceData.delete(new ParDataId(p.getCriacao(), id));

        return super.delete(id);
    }
}
