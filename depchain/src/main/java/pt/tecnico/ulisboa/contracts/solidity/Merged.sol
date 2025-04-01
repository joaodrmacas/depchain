// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract Merged is ERC20, Ownable {
    // Blacklist mapping
    mapping(address => bool) private _blacklist;

    // Exchange rate: tokens per ETH
    uint256 public tokenPricePerEth = 230000; // 1 ETH = 230000 IST tokens

    // Events for blacklist changes
    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);
    event TokensPurchased(address indexed buyer, uint256 ethAmount, uint256 tokenAmount);

    constructor() ERC20("IST Coin", "IST") Ownable(msg.sender) {
        // Initial token supply of 100 million with 2 decimals
        uint256 initialSupply = 100_000_000 * (10 ** 2);
        _mint(msg.sender, initialSupply);
    }

    // Override decimals to set to 2
    function decimals() public view virtual override returns (uint8) {
        return 2;
    }

    // Blacklist Management Functions
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

    // Override transfer to add blacklist check
    function transfer(address to, uint256 value) public virtual override returns (bool) {
        require(!_blacklist[msg.sender], "Sender is blacklisted");
        require(!_blacklist[to], "Recipient is blacklisted");

        return super.transfer(to, value);
    }

    // Override transferFrom to add blacklist check
    function transferFrom(address from, address to, uint256 value) public virtual override returns (bool) {
        require(!_blacklist[msg.sender], "Sender is blacklisted");
        require(!_blacklist[from], "From address is blacklisted");
        require(!_blacklist[to], "Recipient is blacklisted");

        return super.transferFrom(from, to, value);
    }

    // Override approve to add blacklist check
    function approve(address spender, uint256 value) public virtual override returns (bool) {
        require(!_blacklist[msg.sender], "Sender is blacklisted");
        require(!_blacklist[spender], "Spender is blacklisted");

        return super.approve(spender, value);
    }

    // Function to buy IST tokens with ETH
    function buy() external payable {
        require(msg.sender != owner(), "Owner cannot buy tokens");
        require(msg.value > 0, "Must send ETH to buy tokens");
        require(!_blacklist[msg.sender], "Buyer is blacklisted");
        
        // Calculate token amount based on ETH sent and the token price
        uint256 tokenAmount = msg.value * tokenPricePerEth;
        
        // Adjust for decimals (IST has 2 decimals)
        //TODO: not sure se isto ta bem
        tokenAmount = tokenAmount * (10 ** decimals()) / (10 ** 18);
        
        // Check if owner has enough tokens
        address ownerAddress = owner();
        require(balanceOf(ownerAddress) >= tokenAmount, "Not enough tokens available for sale");
        
        // Directly transfer tokens from owner to buyer
        // This uses the internal _transfer function which doesn't require approval
        _transfer(ownerAddress, msg.sender, tokenAmount);
        
        // Transfer ETH to the owner
        (bool sent, ) = payable(ownerAddress).call{value: msg.value}("");
        require(sent, "Failed to send ETH to owner");
        
        emit TokensPurchased(msg.sender, msg.value, tokenAmount);
    }
}