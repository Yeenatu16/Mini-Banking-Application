1. MiniBanking Application


   2. Table of Contents

           - Features
          - Database Setup
          - Running the Application
          - License

       2.1 Features

        Account Management
          - Create a new account with a unique 14-digit account number (e.g., `1000-000000001`).
          - Alphanumeric passwords for enhanced security.
          - Login to an existing account using account number + password.

       Banking Operations
          - Deposit: Add funds to the account.
          - Withdraw: Withdraw funds from the account.
          - Transfer:Send money to another account.
          - Check Balance: View current account balance.

      Transaction Logging
        - Every operation is recorded with:
        - Transaction type (DEPOSIT, WITHDRAW, TRANSFER)
        - Account numbers involved
        - Amount, balance before & after
        - Status (SUCCESS, FAILED, PENDING)
        - Notes and timestamp

     Error Handling
          - Prevents invalid deposit, withdrawal, and transfer amounts.
          - Rolls back transactions in case of failure.
          - Provides user-friendly messages for errors.


     2.3 Database Setup

    1. Create a database called bank:

     sql-statement
        CREATE DATABASE bank;
        USE bank;
    2. Create a TABLE called customer:

      sql-statement

        CREATE TABLE customer (
            ac_no VARCHAR(14) NOT NULL PRIMARY KEY,
            cname VARCHAR(45) UNIQUE NOT NULL,
            balance DECIMAL(15,2) NOT NULL,
            pass_code VARCHAR(50) NOT NULL
        ) ENGINE=InnoDB;



    3.Create a TABLE called transactions:

       sql-statement

        CREATE TABLE transactions (
            tran_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            tran_ref VARCHAR(50) NOT NULL,
            tran_type ENUM('DEPOSIT','WITHDRAW','TRANSFER') NOT NULL,
            ac_no_from VARCHAR(14) NULL,
            ac_no_to VARCHAR(14) NULL,
            amount DECIMAL(15,2) NOT NULL,
            balance_before DECIMAL(15,2) NULL,
            balance_after DECIMAL(15,2) NULL,
            status ENUM('SUCCESS','FAILED','PENDING') DEFAULT 'SUCCESS',
            note VARCHAR(255) NULL,
            created_by VARCHAR(100) NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_ac_from (ac_no_from),
            INDEX idx_ac_to (ac_no_to),
            INDEX idx_tran_ref (tran_ref)
        ) ENGINE=InnoDB;


      2.3 Running the Application

    ->The application allows users to create accounts, login, deposit, withdraw, transfer, check balance, and view transaction history.

    ->Transactions are logged automatically with full details and error handling.


     2.4 License

        This project is licensed under the MIT License.
       .
