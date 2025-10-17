import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

/**
 * HashTableProject.java
 *
 * Implementa duas abordagens de tabela hash: encadeamento (chaining) e endere\u00e7amento aberto
 * (com sondagem linear, quadr\u00e1tica e duplo-hash). Mede tempo de inser\u00e7\u00e3o e busca,
 * conta colis\u00f5es, produz estat\u00edsticas das maiores listas encadeadas e gaps (espa\u00e7os vazios)
 * no vetor.
 *
 * OBS: Esse c\u00f3digo prioriza clareza e facilidade de extens\u00e3o para o trabalho da disciplina.
 * Gera arquivos CSV com dados brutos para plotagem e um sum\u00e1rio no console.
 *
 * Como usar:
 *  - compilar: javac HashTableProject.java
 *  - executar: java HashTableProject
 *
 * Par\u00e2metros e configura\u00e7\u00f5es s\u00e3o definidos em MAIN_CONFIG abaixo.
 *
 * Aten\u00e7\u00e3o: gerar 10 milhões de registros e rodar todas as combina\u00e7\u00f5es requer muita RAM e tempo.
 * Recomenda-se testar com 100k e 1M localmente; 10M requer m\u00e1quina com grande heap (-Xmx) ou execução em servidor.
 */

class Registro {
    public final String codigo; // 9 dígitos como string (leading zeros allowed)
    public Registro(String codigo) { this.codigo = codigo; }
    @Override public String toString() { return codigo; }
    @Override public int hashCode() { return Integer.parseInt(codigo); }
}

// --- Hash Function utilities ---
interface HashFunction {
    int hash(String key, int tableSize);
    String name();
}

class HashMod implements HashFunction {
    public int hash(String key, int tableSize) {
        int k = Integer.parseInt(key);
        return Math.floorMod(k, tableSize);
    }
    public String name(){ return "mod"; }
}

class HashMult implements HashFunction {
    // Multiplication method (Knuth): floor(m * frac(k*A)) where A ~ (sqrt(5)-1)/2
    private final double A = (Math.sqrt(5)-1)/2.0;
    public int hash(String key, int tableSize){
        int k = Integer.parseInt(key);
        double frac = (k * A) % 1.0;
        return (int)Math.floor(tableSize * frac);
    }
    public String name(){ return "mult"; }
}

class HashJava implements HashFunction {
    public int hash(String key, int tableSize){
        int h = key.hashCode();
        return Math.floorMod(h, tableSize);
    }
    public String name(){ return "java"; }
}

// --- Chaining Hash Table ---
class HashTableChaining {
    private final LinkedList<Registro>[] table;
    private final int size;
    public long collisions = 0; // count when inserting into an already non-empty bucket

    @SuppressWarnings("unchecked")
    public HashTableChaining(int size){
        this.size = size;
        table = (LinkedList<Registro>[]) new LinkedList[size];
        for(int i=0;i<size;i++) table[i] = new LinkedList<>();
    }

    public void insert(Registro r, HashFunction hf){
        int idx = hf.hash(r.codigo, size);
        LinkedList<Registro> bucket = table[idx];
        if(!bucket.isEmpty()) collisions++; // count collision when bucket already has at least one element
        bucket.add(r);
    }

    public boolean contains(Registro r, HashFunction hf){
        int idx = hf.hash(r.codigo, size);
        LinkedList<Registro> bucket = table[idx];
        for(Registro x: bucket) if(x.codigo.equals(r.codigo)) return true;
        return false;
    }

    public List<Integer> chainLengths(){
        List<Integer> out = new ArrayList<>(size);
        for(LinkedList<Registro> b: table) out.add(b.size());
        return out;
    }

    public int occupiedSlots(){
        int c=0; for(LinkedList<Registro> b: table) if(!b.isEmpty()) c++; return c;
    }

    // gaps: lengths of consecutive empty slots
    public List<Integer> gaps(){
        List<Integer> g = new ArrayList<>();
        int cur=0;
        for(LinkedList<Registro> b: table){
            if(b.isEmpty()) cur++; else { if(cur>0) g.add(cur); cur=0; }
        }
        if(cur>0) g.add(cur);
        return g;
    }
}

// --- Open Addressing (general) ---
class HashTableOpenAddressing {
    public enum Probe {LINEAR, QUADRATIC, DOUBLE}
    private final Registro[] table;
    private final int size;
    public long collisions = 0; // number of probes (excluding the successful probe?) We'll count probes - 1 as collisions

    public HashTableOpenAddressing(int size){ this.size=size; table = new Registro[size]; }

    private int probeIndex(String key, int i, int h1, int h2, Probe p){
        switch(p){
            case LINEAR: return Math.floorMod(h1 + i, size);
            case QUADRATIC: return Math.floorMod(h1 + i + 3*i*i, size);
            case DOUBLE: return Math.floorMod(h1 + i * (h2==0?1:h2), size);
            default: return Math.floorMod(h1 + i, size);
        }
    }

    // insert returns number of probes used (for counting collisions)
    public int insert(Registro r, HashFunction hf1, HashFunction hf2, Probe p){
        int h1 = hf1.hash(r.codigo, size);
        int h2 = hf2==null?0:hf2.hash(r.codigo, size);
        for(int i=0;i<size;i++){
            int idx = probeIndex(r.codigo, i, h1, h2, p);
            if(table[idx]==null){
                table[idx]=r;
                collisions += i; // i probes before finding empty => i collisions
                return i+1; // probes used
            }
        }
        // table full
        return -1;
    }

    public boolean contains(Registro r, HashFunction hf1, HashFunction hf2, Probe p){
        int h1 = hf1.hash(r.codigo, size);
        int h2 = hf2==null?0:hf2.hash(r.codigo, size);
        for(int i=0;i<size;i++){
            int idx = probeIndex(r.codigo, i, h1, h2, p);
            Registro cur = table[idx];
            if(cur==null) return false; // empty slot -> not present
            if(cur.codigo.equals(r.codigo)) return true;
        }
        return false;
    }

    public List<Integer> gaps(){
        List<Integer> g = new ArrayList<>(); int cur=0;
        for(Registro r: table){ if(r==null) cur++; else { if(cur>0) g.add(cur); cur=0; } }
        if(cur>0) g.add(cur); return g;
    }

    public int occupiedSlots(){int c=0; for(Registro r: table) if(r!=null) c++; return c; }

    public List<Integer> clusterLengths(){
        // cluster = consecutive occupied slots
        List<Integer> clusters = new ArrayList<>(); int cur=0;
        for(Registro r: table){ if(r!=null) cur++; else { if(cur>0) clusters.add(cur); cur=0; } }
        if(cur>0) clusters.add(cur); return clusters;
    }
}

// --- Main experiment controller ---
public class HashTableProject {

    // ---------- CONFIGURABLE ----------
    static final int[] TABLE_SIZES = {1000, 10000, 100000}; // must be x10 progression
    static final int[] DATASET_SIZES = {100_000, 1_000_000}; // remove 10_000_000 by default to avoid OOM; user can edit to add 10_000_000
    static final long[] SEEDS = {42L, 4242L, 424242L}; // use seeds so each hash function sees same data when required
    static final String OUTPUT_DIR = "results";
    // Which hash functions to use:
    static final HashFunction HF_MOD = new HashMod();
    static final HashFunction HF_MULT = new HashMult();
    static final HashFunction HF_JAVA = new HashJava();
    // For open addressing double-hash we can use two different hash functions (hf1 and hf2)
    // ----------------------------------

    // Utility: generate dataset of n registros using given seed (returns array of Registro)
    static Registro[] generateDataset(int n, long seed) {
        Registro[] arr = new Registro[n];
        Random rnd = new Random(seed);
        for(int i=0;i<n;i++){
            int value = rnd.nextInt(1_000_000_000); // up to 9 digits
            String code = String.format("%09d", value);
            arr[i] = new Registro(code);
        }
        return arr;
    }

    // write simple CSV of summary
    static void writeCSV(String path, List<String> lines) throws IOException{
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        try(BufferedWriter w = new BufferedWriter(new FileWriter(path))){
            for(String l: lines) w.write(l+"\n");
        }
    }

    public static void main(String[] args) throws Exception{
        System.out.println("HashTableProject - Iniciando experimento\n");
        Files.createDirectories(Paths.get(OUTPUT_DIR));

        // prepare hash functions lists for experiments
        HashFunction[] hfList = new HashFunction[]{HF_MOD, HF_MULT, HF_JAVA};
        String[] hfNames = new String[]{HF_MOD.name(), HF_MULT.name(), HF_JAVA.name()};

        // CSV header for raw results
        List<String> csvLines = new ArrayList<>();
        csvLines.add("datasetSize,seed,tableSize,structure,hashA,hashB,probeType,insertionTimeMs,insertionCollisions,searchTimeMs,searchFound,occupiedSlots,largestChainOrCluster,smallestGap,largestGap,avgGap");

        for(int ds: DATASET_SIZES){
            for(long seed: SEEDS){
                System.out.println("Gerando dataset (n="+ds+", seed="+seed+")");
                Registro[] data = generateDataset(ds, seed);
                System.out.println("Gerado "+data.length+" registros.\n");

                for(int tableSize: TABLE_SIZES){
                    System.out.println("-> Tabela size="+tableSize);

                    // 1) Chaining with each hash function
                    for(HashFunction hf: hfList){
                        System.gc(); Thread.sleep(50);
                        HashTableChaining ht = new HashTableChaining(tableSize);
                        long t0 = System.nanoTime();
                        for(Registro r: data) ht.insert(r, hf);
                        long t1 = System.nanoTime();
                        long insertMs = (t1-t0)/1_000_000;

                        // search all
                        long s0 = System.nanoTime(); int found=0;
                        for(Registro r: data) if(ht.contains(r, hf)) found++;
                        long s1 = System.nanoTime();
                        long searchMs = (s1-s0)/1_000_000;

                        List<Integer> chains = ht.chainLengths();
                        int largest = chains.stream().mapToInt(Integer::intValue).max().orElse(0);
                        List<Integer> gaps = ht.gaps();
                        int smallestGap = gaps.isEmpty()?0:gaps.stream().mapToInt(Integer::intValue).min().getAsInt();
                        int largestGap = gaps.isEmpty()?0:gaps.stream().mapToInt(Integer::intValue).max().getAsInt();
                        double avgGap = gaps.isEmpty()?0:gaps.stream().mapToInt(Integer::intValue).average().orElse(0.0);

                        csvLines.add(String.join(",", Arrays.asList(
                                String.valueOf(ds), String.valueOf(seed), String.valueOf(tableSize), "chaining",
                                hf.name(), "-", "-",
                                String.valueOf(insertMs), String.valueOf(ht.collisions), String.valueOf(searchMs), String.valueOf(found), String.valueOf(ht.occupiedSlots()), String.valueOf(largest), String.valueOf(smallestGap), String.valueOf(largestGap), new DecimalFormat("0.00").format(avgGap)
                        )));

                        System.out.println(String.format("chaining | hf=%s | insertMs=%d | coll=%d | searchMs=%d | found=%d | largestChain=%d",
                                hf.name(), insertMs, ht.collisions, searchMs, found, largest));
                    }

                    // 2) Open addressing: linear, quadratic, double (hash double uses two functions)
                    for(HashFunction hfPrimary: hfList){
                        for(HashTableOpenAddressing.Probe probe: HashTableOpenAddressing.Probe.values()){
                            // For DOUBLE probe, choose a secondary hash different from primary (simple choice)
                            HashFunction hfSecondary = (probe==HashTableOpenAddressing.Probe.DOUBLE)?
                                    (hfPrimary==HF_MOD?HF_MULT:HF_MOD) : null;

                            System.gc(); Thread.sleep(50);
                            HashTableOpenAddressing ht = new HashTableOpenAddressing(tableSize);
                            long t0 = System.nanoTime();
                            int fails = 0;
                            for(Registro r: data){
                                int probes = ht.insert(r, hfPrimary, hfSecondary, probe);
                                if(probes==-1) { fails++; }
                            }
                            long t1 = System.nanoTime();
                            long insertMs = (t1-t0)/1_000_000;

                            // search all
                            long s0 = System.nanoTime(); int found=0;
                            for(Registro r: data) if(ht.contains(r, hfPrimary, hfSecondary, probe)) found++;
                            long s1 = System.nanoTime(); long searchMs = (s1-s0)/1_000_000;

                            List<Integer> clusters = ht.clusterLengths();
                            int largestCluster = clusters.stream().mapToInt(Integer::intValue).max().orElse(0);
                            List<Integer> gaps = ht.gaps();
                            int smallestGap = gaps.isEmpty()?0:gaps.stream().mapToInt(Integer::intValue).min().getAsInt();
                            int largestGap = gaps.isEmpty()?0:gaps.stream().mapToInt(Integer::intValue).max().getAsInt();
                            double avgGap = gaps.isEmpty()?0:gaps.stream().mapToInt(Integer::intValue).average().orElse(0.0);

                            csvLines.add(String.join(",", Arrays.asList(
                                    String.valueOf(ds), String.valueOf(seed), String.valueOf(tableSize), "open",
                                    hfPrimary.name(), (hfSecondary==null?"-":hfSecondary.name()), probe.name(),
                                    String.valueOf(insertMs), String.valueOf(ht.collisions), String.valueOf(searchMs), String.valueOf(found), String.valueOf(ht.occupiedSlots()), String.valueOf(largestCluster), String.valueOf(smallestGap), String.valueOf(largestGap), new DecimalFormat("0.00").format(avgGap)
                            )));

                            System.out.println(String.format("open | hf=%s | probe=%s | insertMs=%d | coll=%d | searchMs=%d | found=%d | largestCluster=%d | fails=%d",
                                    hfPrimary.name(), probe.name(), insertMs, ht.collisions, searchMs, found, largestCluster, fails));
                        }
                    }

                    // flush intermediate CSV per table size to avoid huge memory usage
                    writeCSV(OUTPUT_DIR + "/results_ds" + ds + "_t" + tableSize + ".csv", csvLines);
                }

            }
        }

        // final write
        writeCSV(OUTPUT_DIR + "/results_summary.csv", csvLines);
        System.out.println("\nExperimento completo. CSVs gerados na pasta '"+OUTPUT_DIR+"'.\n");
        System.out.println("Observações:\n - Para 10.000.000 registros ajuste DATASET_SIZES no topo do arquivo e garanta heap suficiente (ex: -Xmx20g).\n - Use as CSVs para gerar gráficos (Excel, Python/matplotlib, R).\n");
    }
}
