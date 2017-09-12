import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        UTXOPool validatedUTXOsPool = new UTXOPool();
        int inSum = 0;
        int outSum = 0;

        for (int i = 0 ; i < tx.numInputs() ; i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxoToValidate = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = utxoPool.getTxOutput(utxoToValidate);

            // condition 1
            if (!utxoPool.contains(utxoToValidate)) return false;

            // condition 2
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) return false;

            // condition 3
            if (validatedUTXOsPool.contains(utxoToValidate)) return false;

            // condition 4
            if (tx.getOutput(input.outputIndex).value < 0) return false;

            validatedUTXOsPool.addUTXO(utxoToValidate, tx.getOutput(input.outputIndex));
            inSum += output.value;
        }

        for (Transaction.Output output : tx.getOutputs()) {
            outSum += output.value;
        }

        return inSum >= outSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> transactions = new ArrayList<>();

        for (Transaction tx : possibleTxs) {
            if (!isValidTx(tx)) {
                transactions.add(tx);
                for (Transaction.Input in : tx.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output out = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, out);
                }
            }
        }

        return transactions.toArray(new Transaction[transactions.size()]);
    }

}
