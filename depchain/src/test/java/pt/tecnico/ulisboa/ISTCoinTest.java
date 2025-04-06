package pt.tecnico.ulisboa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.checkerframework.checker.units.qual.C;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.junit.Before;
import org.junit.Test;

import pt.tecnico.ulisboa.utils.ContractUtils;

public class ISTCoinTest {
    private ByteArrayOutputStream output;
    private EVMExecutor executor;
    private SimpleWorld world;
    private Address owner;
    private Address user1;
    private Address user2;
    private Address blacklistedUser;
    private Address contractAddress;

    // Contract bytecode and method signatures
    private static final Bytes CONTRACT_BYTECODE = Bytes.fromHexString(
            "0x6080604052600436106100fd575f3560e01c8063715018a611610094578063a6f2ae3a11610063578063a6f2ae3a14610317578063a9059cbb14610321578063dd62ed3e1461035d578063f2fde38b14610399578063fe575a87146103c1576100fd565b8063715018a6146102835780638181515d146102995780638da5cb5b146102c357806395d89b41146102ed576100fd565b8063313ce567116100d0578063313ce567146101cd57806344337ea1146101f7578063537df3b61461021f57806370a0823114610247576100fd565b806306fdde0314610101578063095ea7b31461012b57806318160ddd1461016757806323b872dd14610191575b5f80fd5b34801561010c575f80fd5b506101156103fd565b60405161012291906116cd565b60405180910390f35b348015610136575f80fd5b50610151600480360381019061014c919061177e565b61048d565b60405161015e91906117d6565b60405180910390f35b348015610172575f80fd5b5061017b610532565b60405161018891906117fe565b60405180910390f35b34801561019c575f80fd5b506101b760048036038101906101b29190611817565b61053b565b6040516101c491906117d6565b60405180910390f35b3480156101d8575f80fd5b506101e161062b565b6040516101ee9190611882565b60405180910390f35b348015610202575f80fd5b5061021d6004803603810190610218919061189b565b610633565b005b34801561022a575f80fd5b506102456004803603810190610240919061189b565b6107ce565b005b348015610252575f80fd5b5061026d6004803603810190610268919061189b565b610967565b60405161027a91906117fe565b60405180910390f35b34801561028e575f80fd5b506102976109ac565b005b3480156102a4575f80fd5b506102ad6109bf565b6040516102ba91906117fe565b60405180910390f35b3480156102ce575f80fd5b506102d76109c5565b6040516102e491906118d5565b60405180910390f35b3480156102f8575f80fd5b506103016109ed565b60405161030e91906116cd565b60405180910390f35b61031f610a7d565b005b34801561032c575f80fd5b506103476004803603810190610342919061177e565b610d20565b60405161035491906117d6565b60405180910390f35b348015610368575f80fd5b50610383600480360381019061037e91906118ee565b610dc5565b60405161039091906117fe565b60405180910390f35b3480156103a4575f80fd5b506103bf60048036038101906103ba919061189b565b610e47565b005b3480156103cc575f80fd5b506103e760048036038101906103e2919061189b565b610ecb565b6040516103f491906117d6565b60405180910390f35b60606003805461040c90611959565b80601f016020809104026020016040519081016040528092919081815260200182805461043890611959565b80156104835780601f1061045a57610100808354040283529160200191610483565b820191905f5260205f20905b81548152906001019060200180831161046657829003601f168201915b5050505050905090565b5f61049733610ecb565b156104d7576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016104ce906119d3565b60405180910390fd5b6104e083610ecb565b15610520576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161051790611a3b565b60405180910390fd5b61052a8383610f1d565b905092915050565b5f600254905090565b5f61054533610ecb565b15610585576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161057c906119d3565b60405180910390fd5b61058e84610ecb565b156105ce576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016105c590611aa3565b60405180910390fd5b6105d783610ecb565b15610617576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161060e90611b0b565b60405180910390fd5b610622848484610f3f565b90509392505050565b5f6002905090565b61063b610f6d565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16036106a9576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016106a090611b73565b60405180910390fd5b60065f8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff1615610733576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161072a90611bdb565b60405180910390fd5b600160065f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167ff9b68063b051b82957fa193585681240904fed808db8b30fc5a2d2202c6ed62760405160405180910390a250565b6107d6610f6d565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610844576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161083b90611b73565b60405180910390fd5b60065f8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff166108cd576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016108c490611c43565b60405180910390fd5b5f60065f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167f2b6bf71b58b3583add364b3d9060ebf8019650f65f5be35f5464b9cb3e4ba2d460405160405180910390a250565b5f805f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20549050919050565b6109b4610f6d565b6109bd5f610ff4565b565b60075481565b5f60055f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b6060600480546109fc90611959565b80601f0160208091040260200160405190810160405280929190818152602001828054610a2890611959565b8015610a735780601f10610a4a57610100808354040283529160200191610a73565b820191905f5260205f20905b815481529060010190602001808311610a5657829003601f168201915b5050505050905090565b610a856109c5565b73ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1603610af2576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610ae990611cab565b60405180910390fd5b5f3411610b34576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610b2b90611d13565b60405180910390fd5b610b3d33610ecb565b15610b7d576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610b7490611d7b565b60405180910390fd5b5f60075434610b8c9190611dc6565b9050670de0b6b3a7640000610b9f61062b565b600a610bab9190611f36565b82610bb69190611dc6565b610bc09190611fad565b90505f610bcb6109c5565b905081610bd782610967565b1015610c18576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610c0f9061204d565b60405180910390fd5b610c238133846110b7565b5f8173ffffffffffffffffffffffffffffffffffffffff1634604051610c4890612098565b5f6040518083038185875af1925050503d805f8114610c82576040519150601f19603f3d011682016040523d82523d5f602084013e610c87565b606091505b5050905080610ccb576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610cc2906120f6565b60405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff167f8fafebcaf9d154343dad25669bfa277f4fbacd7ac6b0c4fed522580e040a0f333485604051610d13929190612114565b60405180910390a2505050565b5f610d2a33610ecb565b15610d6a576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610d61906119d3565b60405180910390fd5b610d7383610ecb565b15610db3576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610daa90611b0b565b60405180910390fd5b610dbd83836111a7565b905092915050565b5f60015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2054905092915050565b610e4f610f6d565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610ebf575f6040517f1e4fbdf7000000000000000000000000000000000000000000000000000000008152600401610eb691906118d5565b60405180910390fd5b610ec881610ff4565b50565b5f60065f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff169050919050565b5f80610f276111c9565b9050610f348185856111d0565b600191505092915050565b5f80610f496111c9565b9050610f568582856111e2565b610f618585856110b7565b60019150509392505050565b610f756111c9565b73ffffffffffffffffffffffffffffffffffffffff16610f936109c5565b73ffffffffffffffffffffffffffffffffffffffff1614610ff257610fb66111c9565b6040517f118cdaa7000000000000000000000000000000000000000000000000000000008152600401610fe991906118d5565b60405180910390fd5b565b5f60055f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508160055f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603611127575f6040517f96c6fd1e00000000000000000000000000000000000000000000000000000000815260040161111e91906118d5565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1603611197575f6040517fec442f0500000000000000000000000000000000000000000000000000000000815260040161118e91906118d5565b60405180910390fd5b6111a2838383611275565b505050565b5f806111b16111c9565b90506111be8185856110b7565b600191505092915050565b5f33905090565b6111dd838383600161148e565b505050565b5f6111ed8484610dc5565b90507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff81101561126f5781811015611260578281836040517ffb8f41b20000000000000000000000000000000000000000000000000000000081526004016112579392919061213b565b60405180910390fd5b61126e84848484035f61148e565b5b50505050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff16036112c5578060025f8282546112b99190612170565b92505081905550611393565b5f805f8573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205490508181101561134e578381836040517fe450d38c0000000000000000000000000000000000000000000000000000000081526004016113459392919061213b565b60405180910390fd5b8181035f808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2081905550505b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036113da578060025f8282540392505081905550611424565b805f808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f82825401925050819055505b8173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef8360405161148191906117fe565b60405180910390a3505050565b5f73ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff16036114fe575f6040517fe602df050000000000000000000000000000000000000000000000000000000081526004016114f591906118d5565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff160361156e575f6040517f94280d6200000000000000000000000000000000000000000000000000000000815260040161156591906118d5565b60405180910390fd5b8160015f8673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f8573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20819055508015611657578273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b9258460405161164e91906117fe565b60405180910390a35b50505050565b5f81519050919050565b5f82825260208201905092915050565b8281835e5f83830152505050565b5f601f19601f8301169050919050565b5f61169f8261165d565b6116a98185611667565b93506116b9818560208601611677565b6116c281611685565b840191505092915050565b5f6020820190508181035f8301526116e58184611695565b905092915050565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f61171a826116f1565b9050919050565b61172a81611710565b8114611734575f80fd5b50565b5f8135905061174581611721565b92915050565b5f819050919050565b61175d8161174b565b8114611767575f80fd5b50565b5f8135905061177881611754565b92915050565b5f8060408385031215611794576117936116ed565b5b5f6117a185828601611737565b92505060206117b28582860161176a565b9150509250929050565b5f8115159050919050565b6117d0816117bc565b82525050565b5f6020820190506117e95f8301846117c7565b92915050565b6117f88161174b565b82525050565b5f6020820190506118115f8301846117ef565b92915050565b5f805f6060848603121561182e5761182d6116ed565b5b5f61183b86828701611737565b935050602061184c86828701611737565b925050604061185d8682870161176a565b9150509250925092565b5f60ff82169050919050565b61187c81611867565b82525050565b5f6020820190506118955f830184611873565b92915050565b5f602082840312156118b0576118af6116ed565b5b5f6118bd84828501611737565b91505092915050565b6118cf81611710565b82525050565b5f6020820190506118e85f8301846118c6565b92915050565b5f8060408385031215611904576119036116ed565b5b5f61191185828601611737565b925050602061192285828601611737565b9150509250929050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f600282049050600182168061197057607f821691505b6020821081036119835761198261192c565b5b50919050565b7f53656e64657220697320626c61636b6c697374656400000000000000000000005f82015250565b5f6119bd601583611667565b91506119c882611989565b602082019050919050565b5f6020820190508181035f8301526119ea816119b1565b9050919050565b7f5370656e64657220697320626c61636b6c6973746564000000000000000000005f82015250565b5f611a25601683611667565b9150611a30826119f1565b602082019050919050565b5f6020820190508181035f830152611a5281611a19565b9050919050565b7f46726f6d206164647265737320697320626c61636b6c697374656400000000005f82015250565b5f611a8d601b83611667565b9150611a9882611a59565b602082019050919050565b5f6020820190508181035f830152611aba81611a81565b9050919050565b7f526563697069656e7420697320626c61636b6c697374656400000000000000005f82015250565b5f611af5601883611667565b9150611b0082611ac1565b602082019050919050565b5f6020820190508181035f830152611b2281611ae9565b9050919050565b7f496e76616c6964206164647265737300000000000000000000000000000000005f82015250565b5f611b5d600f83611667565b9150611b6882611b29565b602082019050919050565b5f6020820190508181035f830152611b8a81611b51565b9050919050565b7f4164647265737320616c726561647920626c61636b6c697374656400000000005f82015250565b5f611bc5601b83611667565b9150611bd082611b91565b602082019050919050565b5f6020820190508181035f830152611bf281611bb9565b9050919050565b7f41646472657373206e6f7420626c61636b6c69737465640000000000000000005f82015250565b5f611c2d601783611667565b9150611c3882611bf9565b602082019050919050565b5f6020820190508181035f830152611c5a81611c21565b9050919050565b7f4f776e65722063616e6e6f742062757920746f6b656e730000000000000000005f82015250565b5f611c95601783611667565b9150611ca082611c61565b602082019050919050565b5f6020820190508181035f830152611cc281611c89565b9050919050565b7f4d7573742073656e642045544820746f2062757920746f6b656e7300000000005f82015250565b5f611cfd601b83611667565b9150611d0882611cc9565b602082019050919050565b5f6020820190508181035f830152611d2a81611cf1565b9050919050565b7f427579657220697320626c61636b6c69737465640000000000000000000000005f82015250565b5f611d65601483611667565b9150611d7082611d31565b602082019050919050565b5f6020820190508181035f830152611d9281611d59565b9050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f611dd08261174b565b9150611ddb8361174b565b9250828202611de98161174b565b91508282048414831517611e0057611dff611d99565b5b5092915050565b5f8160011c9050919050565b5f808291508390505b6001851115611e5c57808604811115611e3857611e37611d99565b5b6001851615611e475780820291505b8081029050611e5585611e07565b9450611e1c565b94509492505050565b5f82611e745760019050611f2f565b81611e81575f9050611f2f565b8160018114611e975760028114611ea157611ed0565b6001915050611f2f565b60ff841115611eb357611eb2611d99565b5b8360020a915084821115611eca57611ec9611d99565b5b50611f2f565b5060208310610133831016604e8410600b8410161715611f055782820a905083811115611f0057611eff611d99565b5b611f2f565b611f128484846001611e13565b92509050818404811115611f2957611f28611d99565b5b81810290505b9392505050565b5f611f408261174b565b9150611f4b83611867565b9250611f787fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8484611e65565b905092915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601260045260245ffd5b5f611fb78261174b565b9150611fc28361174b565b925082611fd257611fd1611f80565b5b828204905092915050565b7f4e6f7420656e6f75676820746f6b656e7320617661696c61626c6520666f72205f8201527f73616c6500000000000000000000000000000000000000000000000000000000602082015250565b5f612037602483611667565b915061204282611fdd565b604082019050919050565b5f6020820190508181035f8301526120648161202b565b9050919050565b5f81905092915050565b50565b5f6120835f8361206b565b915061208e82612075565b5f82019050919050565b5f6120a282612078565b9150819050919050565b7f4661696c656420746f2073656e642045544820746f206f776e657200000000005f82015250565b5f6120e0601b83611667565b91506120eb826120ac565b602082019050919050565b5f6020820190508181035f83015261210d816120d4565b9050919050565b5f6040820190506121275f8301856117ef565b61213460208301846117ef565b9392505050565b5f60608201905061214e5f8301866118c6565b61215b60208301856117ef565b61216860408301846117ef565b949350505050565b5f61217a8261174b565b91506121858361174b565b925082820190508082111561219d5761219c611d99565b5b9291505056fea26469706673582212203f9c3b66f6de967f797b3a87bb7d1349196bf479c18a310ff686968d52f1549c64736f6c634300081a0033");

    // Function signatures
    private static final BigInteger TOKEN_PRICE = BigInteger.valueOf(230000); // 1 ETH = 23000 tokens
    private static final Bytes TRANSFER_SIG = Bytes.fromHexString("0xa9059cbb"); // transfer(address,uint256)
    private static final Bytes TRANSFER_FROM_SIG = Bytes.fromHexString("0x23b872dd"); // transferFrom(address,address,uint256)
    private static final Bytes APPROVE_SIG = Bytes.fromHexString("0x095ea7b3"); // approve(address,uint256)
    private static final Bytes ALLOWANCE_SIG = Bytes.fromHexString("0xdd62ed3e"); // allowance(address,address)
    private static final Bytes BALANCE_OF_SIG = Bytes.fromHexString("0x70a08231"); // balanceOf(address)
    private static final Bytes BUY_SIG = Bytes.fromHexString("0xa6f2ae3a"); // buy()
    private static final Bytes ADD_TO_BLACKLIST_SIG = Bytes.fromHexString("0x44337ea1"); // addToBlacklist(address)
    private static final Bytes REMOVE_FROM_BLACKLIST_SIG = Bytes.fromHexString("0x537df3b6"); // removeFromBlacklist(address)
    private static final Bytes IS_BLACKLISTED_SIG = Bytes.fromHexString("0xfe575a87"); // isBlacklisted(address)

    @Before
    public void setup() {
        // Initialize the EVM executor and world
        world = new SimpleWorld();
        output = new ByteArrayOutputStream();
        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(new StandardJsonTracer(new PrintStream(output), true, true, true, true));

        // Setup test accounts
        owner = Address.fromHexString("0x000000000000000000000000000000000000000b");
        user1 = Address.fromHexString("0x0000000000000000000000000000000000000002");
        user2 = Address.fromHexString("0x0000000000000000000000000000000000000003");
        blacklistedUser = Address.fromHexString("0x0000000000000000000000000000000000000004");
        contractAddress = Address.fromHexString("0x0000000000000000000000000000000000000005");

        // Fund accounts
        MutableAccount ownerAccount = world.createAccount(owner);
        ownerAccount.setBalance(
                Wei.of(new BigInteger("00000000000000000000000000000000000000000000001b1ae4d6e2ef500000", 16)));

        MutableAccount user1Account = world.createAccount(user1);
        user1Account.setBalance(
                Wei.of(new BigInteger("00000000000000000000000000000000000000000000001b1ae4d6e2ef500000", 16)));

        MutableAccount user2Account = world.createAccount(user2);
        user2Account.setBalance(
                Wei.of(new BigInteger("00000000000000000000000000000000000000000000001b1ae4d6e2ef500000", 16)));

        MutableAccount blacklistedUserAccount = world.createAccount(blacklistedUser);
        blacklistedUserAccount.setBalance(
                Wei.of(new BigInteger("00000000000000000000000000000000000000000000001b1ae4d6e2ef500000", 16)));

        // Deploy contract
        MutableAccount contractAccount = world.createAccount(contractAddress);
        contractAccount.setCode(CONTRACT_BYTECODE);

        // Set storage for each key-value pair
        contractAccount.setStorageValue(
                UInt256.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000005"),
                UInt256.fromHexString("0x000000000000000000000000000000000000000000000000000000000000000b"));

        contractAccount.setStorageValue(
                UInt256.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000004"),
                UInt256.fromHexString("0x4953540000000000000000000000000000000000000000000000000000000006"));

        contractAccount.setStorageValue(
                UInt256.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000003"),
                UInt256.fromHexString("0x49535420436f696e000000000000000000000000000000000000000000000010"));

        contractAccount.setStorageValue(
                UInt256.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000002"),
                UInt256.fromHexString("0x00000000000000000000000000000000000000000000000000000002540be400"));

        contractAccount.setStorageValue(
                UInt256.fromHexString("0x9115655cbcdb654012cf1b2f7e5dbf11c9ef14e152a19d5f8ea75a329092d5a6"),
                UInt256.fromHexString("0x00000000000000000000000000000000000000000000000000000002540be400"));

        contractAccount.setStorageValue(
                UInt256.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000007"),
                UInt256.fromHexString("0x0000000000000000000000000000000000000000000000000000000000038270"));

        executor.worldUpdater(world.updater());
        executor.commitWorldState();
    }

    @Test
    public void testBalanceOf() {
        // Call balanceOf for user1
        BigInteger balance = getBalance(user1);

        // Check if the balance is correct
        assertEquals("User1 should have 0 tokens", BigInteger.ZERO, balance);
    }

    @Test
    public void testBuyTokens() {
        // Record initial balances
        BigInteger initialOwnerTokens = getBalance(owner);
        BigInteger initialUser1Tokens = getBalance(user1);
        Wei initialOwnerEth = world.getAccount(owner).getBalance();
        Wei initialUser1Eth = world.getAccount(user1).getBalance();

        BigInteger ethToSend = BigInteger.TEN.pow(18); // 1 ETH

        // User1 buys tokens with 1 ETH
        executor.sender(user1);
        executor.receiver(contractAddress);
        executor.callData(BUY_SIG);
        executor.ethValue(Wei.of(ethToSend));
        MutableAccount contractAccount = world.getAccount(contractAddress);
        executor.code(contractAccount.getCode());
        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        // Check token balances after purchase
        BigInteger expectedTokens = ethToSend
                .multiply(TOKEN_PRICE)
                .divide(BigInteger.TEN.pow(18)) // Adjust for ETH decimals
                .multiply(BigInteger.TEN.pow(2)); // Adjust for token decimals

        BigInteger finalUser1Tokens = getBalance(user1);
        BigInteger finalOwnerTokens = getBalance(owner);

        assertEquals("Owner token balance should decrease by tokens sold",
                initialOwnerTokens.subtract(expectedTokens), finalOwnerTokens);

        assertEquals("User1 token balance should increase by tokens bought",
                initialUser1Tokens.add(expectedTokens), finalUser1Tokens);

        // Check ETH balances after purchase
        Wei finalOwnerEth = world.getAccount(owner).getBalance();
        Wei finalUser1Eth = world.getAccount(user1).getBalance();

        assertEquals("Owner ETH balance should increase by ETH received",
                initialOwnerEth.add(Wei.of(ethToSend)), finalOwnerEth);

        assertEquals("User1 ETH balance should decrease by ETH sent",
                initialUser1Eth.subtract(Wei.of(ethToSend)), finalUser1Eth);
    }

    @Test
    public void testTokenTransfer() {
        // First, give user1 some tokens (simulating previous purchase)
        BigInteger tokensForUser = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(2)); // 1000 tokens with
        // 2 decimals
        transferTokens(owner, user1, tokensForUser);

        // Record initial balances
        BigInteger initialUser1Balance = getBalance(user1);
        BigInteger initialUser2Balance = getBalance(user2);

        // Transfer tokens from user1 to user2
        BigInteger transferAmount = BigInteger.valueOf(500).multiply(BigInteger.TEN.pow(2)); // 500 tokens
        transferTokens(user1, user2, transferAmount);

        // Check final balances
        BigInteger finalUser1Balance = getBalance(user1);
        BigInteger finalUser2Balance = getBalance(user2);

        assertEquals("User1 balance should decrease by transferred amount",
                initialUser1Balance.subtract(transferAmount), finalUser1Balance);

        assertEquals("User2 balance should increase by transferred amount",
                initialUser2Balance.add(transferAmount), finalUser2Balance);
    }

    @Test
    public void testApproveAndAllowance() {
        // First, give user1 some tokens (simulating previous purchase)
        BigInteger tokensForUser = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(2)); // 1000 tokens with 2
                                                                                             // decimals
        transferTokens(owner, user1, tokensForUser);

        // User1 approves user2 to spend tokens
        BigInteger approveAmount = BigInteger.valueOf(500).multiply(BigInteger.TEN.pow(2)); // 500 tokens
        executor.sender(user1);
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(
                APPROVE_SIG,
                encodeAddress(user2),
                encodeUint256(approveAmount)));
        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        // Check allowance
        executor.callData(Bytes.concatenate(
                ALLOWANCE_SIG,
                encodeAddress(user1),
                encodeAddress(user2)));
        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);
        BigInteger allowance = ContractUtils.extractBigIntegerFromReturnData(output);
        assertEquals("Allowance should be equal to approved amount",
                approveAmount, allowance);
    }

    @Test
    public void testApproveAndTransferFrom() {
        // First, give user1 some tokens (simulating previous purchase)
        BigInteger tokensForUser = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(2)); // 1000 tokens with 2

        // decimals
        transferTokens(owner, user1, tokensForUser);

        // User1 approves user2 to spend tokens
        BigInteger approveAmount = BigInteger.valueOf(500).multiply(BigInteger.TEN.pow(2)); // 500 tokens
        executor.sender(user1);
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(
                APPROVE_SIG,
                encodeAddress(user2),
                encodeUint256(approveAmount)));
        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        // Check allowance
        executor.callData(Bytes.concatenate(
                ALLOWANCE_SIG,
                encodeAddress(user1),
                encodeAddress(user2)));
        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);
        BigInteger allowance = ContractUtils.extractBigIntegerFromReturnData(output);
        assertEquals("Allowance should be equal to approved amount",
                approveAmount, allowance);

        // User2 transfers tokens from user1 to themselves
        BigInteger transferAmount = BigInteger.valueOf(300).multiply(BigInteger.TEN.pow(2)); // 300 tokens
        executor.sender(user2);
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(
                TRANSFER_FROM_SIG,
                encodeAddress(user1),
                encodeAddress(user2),
                encodeUint256(transferAmount)));
        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        // Check final balances
        BigInteger finalUser1Balance = getBalance(user1);
        BigInteger finalUser2Balance = getBalance(user2);
        assertEquals("User1 balance should decrease by transferred amount",
                tokensForUser.subtract(transferAmount), finalUser1Balance);
        assertEquals("User2 balance should increase by transferred amount",
                transferAmount, finalUser2Balance);
    }

    @Test
    public void testBlacklistingAndRemoval() {
        // Add user to blacklist
        executor.sender(owner);
        executor.ethValue(Wei.of(0));
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(ADD_TO_BLACKLIST_SIG,
                encodeAddress(blacklistedUser)));
        executor.code(CONTRACT_BYTECODE);
        executor.execute();
        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        // Check if user is blacklisted
        executor.callData(Bytes.concatenate(IS_BLACKLISTED_SIG,
                encodeAddress(blacklistedUser)));
        executor.execute();
        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);
        boolean isBlacklisted = ContractUtils.extractBooleanFromReturnData(output);
        assertTrue("User should be blacklisted", isBlacklisted);

        // Remove from blacklist
        executor.sender(owner);
        executor.callData(Bytes.concatenate(REMOVE_FROM_BLACKLIST_SIG,
                encodeAddress(blacklistedUser)));
        executor.execute();
        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        // Check if user is still blacklisted
        executor.callData(Bytes.concatenate(IS_BLACKLISTED_SIG,
                encodeAddress(blacklistedUser)));
        executor.execute();
        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);
        boolean isBlacklistedAfterRemoval = ContractUtils.extractBooleanFromReturnData(output);
        assertFalse("User should no longer be blacklisted", isBlacklistedAfterRemoval);
    }

    @Test
    public void testTransferToBlacklistedAddressFails() {
        // Give some tokens to blacklistedUser first
        BigInteger tokensForUser = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(2));
        transferTokens(owner, blacklistedUser, tokensForUser);

        // Record initial balances
        BigInteger initialBlacklistedBalance = getBalance(blacklistedUser);
        assertEquals("Blacklisted user should have 1000 tokens",
                tokensForUser, initialBlacklistedBalance);

        // Add user to blacklist
        executor.sender(owner);
        executor.ethValue(Wei.of(0));
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(ADD_TO_BLACKLIST_SIG,
                encodeAddress(blacklistedUser)));
        executor.code(CONTRACT_BYTECODE);
        executor.execute();
        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        // Verify user is blacklisted
        executor.callData(Bytes.concatenate(IS_BLACKLISTED_SIG,
                encodeAddress(blacklistedUser)));
        executor.execute();
        ContractUtils.checkForExecutionErrors(output);
        boolean isBlacklisted = ContractUtils.extractBooleanFromReturnData(output);
        assertTrue("User should be blacklisted", isBlacklisted);

        // Try to transfer tokens from blacklisted user (should fail)
        try {
            transferTokens(blacklistedUser, owner, tokensForUser);
        } catch (Exception e) {
            // assert that the exception is a runtime exception
            assertTrue("Expected a runtime exception", e instanceof RuntimeException);
        }

        // Check if transfer was successful (should not be)
        BigInteger finalBlacklistedBalance = getBalance(blacklistedUser);
        assertEquals("Blacklisted user balance should remain unchanged after failed transfer",
                initialBlacklistedBalance, finalBlacklistedBalance);
    }

    @Test
    public void testApproveAndTransferFromWithBlacklist() {
        // Give tokens to user1
        BigInteger tokensForUser = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(2));
        transferTokens(owner, user1, tokensForUser);

        // User1 approves user2 to spend tokens
        BigInteger approveAmount = BigInteger.valueOf(500).multiply(BigInteger.TEN.pow(2));
        executor.sender(user1);
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(
                APPROVE_SIG,
                encodeAddress(user2),
                encodeUint256(approveAmount)));
        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        // Record initial balances
        BigInteger initialUser1Balance = getBalance(user1);
        BigInteger initialUser2Balance = getBalance(user2);

        // User2 transfers tokens from user1 to themselves
        BigInteger transferAmount = BigInteger.valueOf(300).multiply(BigInteger.TEN.pow(2));
        executor.sender(user2);
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(
                TRANSFER_FROM_SIG,
                encodeAddress(user1),
                encodeAddress(user2),
                encodeUint256(transferAmount)));
        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        // Check final balances
        BigInteger finalUser1Balance = getBalance(user1);
        BigInteger finalUser2Balance = getBalance(user2);

        assertEquals("User1 balance should decrease by transferred amount",
                initialUser1Balance.subtract(transferAmount), finalUser1Balance);

        assertEquals("User2 balance should increase by transferred amount",
                initialUser2Balance.add(transferAmount), finalUser2Balance);

        // Now blacklist user2 and try again
        executor.sender(owner);
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(ADD_TO_BLACKLIST_SIG,
                encodeAddress(user2)));
        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        // Try transferFrom with blacklisted spender (should fail)
        executor.sender(user2);
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(
                TRANSFER_FROM_SIG,
                encodeAddress(user1),
                encodeAddress(user2),
                encodeUint256(transferAmount)));
        executor.execute();

        // Check for execution errors
        try {
            ContractUtils.checkForExecutionErrors(output);
        } catch (Exception e) {
            assertTrue("Expected a runtime exception", e instanceof RuntimeException);
        }

        // Balances should remain unchanged after failed transfer
        BigInteger afterBlacklistUser1Balance = getBalance(user1);
        BigInteger afterBlacklistUser2Balance = getBalance(user2);

        assertEquals("User1 balance should remain unchanged after failed transfer",
                finalUser1Balance, afterBlacklistUser1Balance);

        assertEquals("User2 balance should remain unchanged after failed transfer",
                finalUser2Balance, afterBlacklistUser2Balance);
    }

    @Test
    public void testOwnerCannotBuyTokens() {
        // Record initial balances
        BigInteger initialOwnerTokens = getBalance(owner);
        Wei initialOwnerEth = world.getAccount(owner).getBalance();

        // Owner tries to buy tokens (should fail)
        BigInteger ethToSend = BigInteger.TEN.pow(18); // 1 ETH

        executor.sender(owner);
        executor.receiver(contractAddress);
        executor.callData(BUY_SIG);
        executor.ethValue(Wei.of(ethToSend));
        executor.execute();

        // Check for execution errors
        try {
            ContractUtils.checkForExecutionErrors(output);
        } catch (Exception e) {
            assertTrue("Expected a runtime exception", e instanceof RuntimeException);
        }

        // Balances should remain unchanged
        BigInteger finalOwnerTokens = getBalance(owner);
        Wei finalOwnerEth = world.getAccount(owner).getBalance();

        assertEquals("Owner token balance should remain unchanged",
                initialOwnerTokens, finalOwnerTokens);

        assertEquals("Owner ETH balance should remain unchanged",
                initialOwnerEth, finalOwnerEth);
    }

    // Helper methods

    private BigInteger getBalance(Address address) {
        // Call balanceOf function
        executor.sender(address);
        executor.ethValue(Wei.ZERO);
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(BALANCE_OF_SIG, encodeAddress(address)));
        executor.code(CONTRACT_BYTECODE);
        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);

        BigInteger returnValue = ContractUtils.extractBigIntegerFromReturnData(output);
        // log the return value
        return returnValue;
    }

    private void transferTokens(Address from, Address to, BigInteger amount) {
        executor.sender(from);
        executor.receiver(contractAddress);
        executor.callData(Bytes.concatenate(
                TRANSFER_SIG,
                encodeAddress(to),
                encodeUint256(amount)));
        executor.code(CONTRACT_BYTECODE);

        executor.execute();

        // Check for execution errors
        ContractUtils.checkForExecutionErrors(output);
    }

    private Bytes encodeAddress(Address address) {
        return Bytes.fromHexString(ContractUtils.padHexStringTo256Bit(address.toHexString()));
    }

    private Bytes encodeUint256(BigInteger value) {
        return Bytes.fromHexString(ContractUtils.padBigIntegerTo256Bit(value));
    }
}