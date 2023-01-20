import React from 'react';
import './bank-list.css';
import Bank from '../../model/bank.model';

interface Props {
    banks: Array<Bank> | undefined;
    activeBankId: string;
    onSelectBank: any;
}

export default function BankList(props: Props) {
  return (
    <ul className="bank-list">
      { props.banks && props.banks
        .sort((a, b) => { return a.name < b.name ? -1 : 1; } )
        .map((bank: Bank, index: number) =>
          <li className={"bank-list-item" + (bank.id === props.activeBankId ? "-active" : "")}
            onClick={ () => { props.onSelectBank(bank); } }
            key={ index }
          >
            { bank.name }
          </li>
        )
      }
    </ul>
  );
}
