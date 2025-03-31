// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract Merged is ERC20, Ownable {
    // Blacklist mapping
    mapping(address => bool) private _blacklist;
    uint256 public depCoinPerIST = 23; // Example rate

    // Events for blacklist changes
    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);
    // Event for buying IST
    event BuyIST(
        address indexed buyer,
        uint256 istAmount,
        uint256 depCoinAmount
    );

    constructor() ERC20("IST Coin", "IST") Ownable(msg.sender) {
        uint256 initialSupply = 100_000_000 * (10 ** 2);
        _mint(msg.sender, initialSupply);
    }

    function decimals() public view virtual override returns (uint8) {
        return 2;
    }

    function addToBlacklist(address account) external onlyOwner {
        require(account != address(0), "Invalid address");
        require(!_blacklist[account], "Address already blacklisted");
        _blacklist[account] = true;
        emit AddedToBlacklist(account);
    }

    function removeFromBlacklist(address account) external onlyOwner {
        require(account != address(0), "Invalid address");
        require(_blacklist[account], "Address not blacklisted");
        _blacklist[account] = false;
        emit RemovedFromBlacklist(account);
    }

    function isBlacklisted(address account) external view returns (bool) {
        return _blacklist[account];
    }

    function transfer(
        address to,
        uint256 value
    ) public virtual override returns (bool) {
        require(!_blacklist[msg.sender], "Sender is blacklisted");
        require(!_blacklist[to], "Recipient is blacklisted");
        return super.transfer(to, value);
    }

    function transferFrom(
        address from,
        address to,
        uint256 value
    ) public virtual override returns (bool) {
        require(!_blacklist[msg.sender], "Sender is blacklisted");
        require(!_blacklist[from], "From address is blacklisted");
        require(!_blacklist[to], "Recipient is blacklisted");
        return super.transferFrom(from, to, value);
    }

    function approve(
        address spender,
        uint256 value
    ) public virtual override returns (bool) {
        require(!_blacklist[msg.sender], "Sender is blacklisted");
        require(!_blacklist[spender], "Spender is blacklisted");
        return super.approve(spender, value);
    }

    function buyIST(uint256 istAmount) external payable {
        require(!_blacklist[msg.sender], "Sender is blacklisted");
        uint256 depCoinRequired = istAmount * depCoinPerIST;
        require(msg.value == depCoinRequired, "Incorrect DepCoin amount sent");

        _transfer(owner(), msg.sender, istAmount);

        // Forward DepCoin to the owner
        payable(owner()).transfer(msg.value);

        emit BuyIST(msg.sender, istAmount, depCoinRequired);
    }
}
