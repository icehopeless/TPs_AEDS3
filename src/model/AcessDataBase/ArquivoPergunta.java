package model.AcessDataBase;

import model.Pergunta;

import java.text.Normalizer;
import  java.util.ArrayList;
import java.util.HashMap;


import Formatter.FormatterString;

public class ArquivoPergunta extends Arquivo<Pergunta>{
    ArvoreBMais<ParIdId> relUsuarioPergunta;
    ArvoreBMais<ParDataId> indiceData;
    ListaInvertida invertedList;

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
    }
    
    private int createRefInvert(Pergunta p) throws Exception{
          String [] keyWords = p.getPalavrasChave().split(";");
          HashMap<String,Float> frequency = new HashMap<>();
          invertedList.incrementaEntidades();

          for(int i=0; i < keyWords.length; i++){
            keyWords[i]  = Normalizer.normalize(keyWords[i].strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
            if(frequency.get(keyWords[i]) == null){
                frequency.put(keyWords[i],1/((float)keyWords.length));
            }else{
                frequency.put(keyWords[i],frequency.get(keyWords[i]) + 1/((float)keyWords.length));
            }            
            
          }
          
          for(int i =0; i <  keyWords.length; i++){
                if(!invertedList.create(keyWords[i], new ElementoLista(p.getId(), frequency.get(keyWords[i])))){
                    invertedList.delete(keyWords[i],p.getId());
                    invertedList.create(keyWords[i], new ElementoLista(p.getId(), frequency.get(keyWords[i])));
                }
          }

        return 1;
    }
    
    public void removeAllKeyWords(Pergunta p)throws Exception{
        String [] keyWords = p.getPalavrasChave().split(";");
        invertedList.decrementaEntidades();
        for(int i =0; i < keyWords.length; i++){
            invertedList.delete(keyWords[i], p.getId());
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
        createRefInvert(p);

        invertedList.print(); 

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
        HashMap<String, Integer> termos = new HashMap<>();
        String[] palavras = consulta.strip().split("\\s+");

        for(int i = 0; i < palavras.length; i++){
            String aux = Normalizer.normalize(palavras[i].strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();

            if(!aux.isEmpty()){
                if(termos.get(aux) == null){
                    termos.put(aux,1);
                }else{
                    termos.put(aux, termos.get(aux)+1);
                }
            }

        }

        int total = invertedList.numeroEntidades();
        if (termos.isEmpty() || total <= 0){
            return resultado;
        }

        //calculo tfxidf
        double idf;
        HashMap<Integer, Double> scores = new HashMap<>();
        for(String termo : termos.keySet()){
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
        boolean ok = super.update(p);
        removeAllKeyWords(p);
        createRefInvert(p);
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
        removeAllKeyWords(p);
        relUsuarioPergunta.delete(new ParIdId(p.getIdUsuario(), id));
        indiceData.delete(new ParDataId(p.getCriacao(), id));

        return super.delete(id);
    }
}
