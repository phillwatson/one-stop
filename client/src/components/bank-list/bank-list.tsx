import React from 'react';

import './bank-list.css';
import Bank from '../../model/bank.model';
import BankCard from '../bank-card/bank-card'

interface Props {
    banks: Array<Bank> | undefined;
    activeBankId: string;
    onSelectBank: any;
}

export default function BankList(props: Props) {
  return (
    <div className="bank-list">
      { props.banks && props.banks
        .sort((a, b) => { return a.name < b.name ? -1 : 1; } )
        .map((bank: Bank, index: number) =>
          <div className="bank-item">
            <BankCard key={ index } bank={ bank } active={ false }/>
          </div>
        )
      }
    </div>
  );
}
