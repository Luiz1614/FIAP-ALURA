package br.com.alura.bytebank.domain.conta;

import br.com.alura.bytebank.ConnectionFactory;
import br.com.alura.bytebank.domain.RegraDeNegocioException;
import br.com.alura.bytebank.domain.cliente.Cliente;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ContaService {
    private Set<Conta> contas = new HashSet<>();

    public Set<Conta> listarContasAbertas() {
        return contas;
    }

    private ConnectionFactory connection;

    public ContaService() {
        this.connection = new ConnectionFactory();
    }

    public BigDecimal consultarSaldo(Integer numeroDaConta) {
        var conta = buscarContaPorNumero(numeroDaConta);
        return conta.getSaldo();
    }

    public void abrir(DadosAberturaConta dadosDaConta) {
        var cliente = new Cliente(dadosDaConta.dadosCliente());
        var conta = new Conta(dadosDaConta.numero(), cliente);
        if (contas.contains(conta)) {
            throw new RegraDeNegocioException("Já existe outra conta aberta com o mesmo número!");
        }

        try {
            String sql = "INSERT INTO al_conta (numero, saldo, nome_cliente, cpf_cliente, email_cliente) VALUES (?, ?, ?, ?, ?)";
            Connection conn = connection.recuperaConexao();

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, conta.getNumero());
            ps.setBigDecimal(2, conta.getSaldo());
            ps.setString(3, cliente.getNome());
            ps.setString(4, cliente.getCpf());
            ps.setString(5, cliente.getEmail());

            ps.execute();
            contas.add(conta);

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar a conta no banco de dados!");
        }


    }

    public void realizarSaque(Integer numeroDaConta, BigDecimal valor) {
        var conta = buscarContaPorNumero(numeroDaConta);
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Valor do saque deve ser superior a zero!");
        }

        if (valor.compareTo(conta.getSaldo()) > 0) {
            throw new RegraDeNegocioException("Saldo insuficiente!");
        }

        conta.sacar(valor);
    }

    public void realizarDeposito(Integer numeroDaConta, BigDecimal valor) {
        var conta = buscarContaPorNumero(numeroDaConta);
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Valor do deposito deve ser superior a zero!");
        }

        conta.depositar(valor);
    }

    public void encerrar(Integer numeroDaConta) {
        var conta = buscarContaPorNumero(numeroDaConta);
        if (conta.possuiSaldo()) {
            throw new RegraDeNegocioException("Conta não pode ser encerrada pois ainda possui saldo!");
        }

        contas.remove(conta);
    }

    private Conta buscarContaPorNumero(Integer numero) {
        return contas
                .stream()
                .filter(c -> c.getNumero() == numero)
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("Não existe conta cadastrada com esse número!"));
    }
}
