//
//  SecondViewController.swift
//  rapidio
//
//  Created by Ethan Fan on 7/29/17.
//  Copyright © 2017 Vimo Labs. All rights reserved.
//

import UIKit
import Rapid

class SecondViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        // subscribe to all to-dos with 'priority' parameter set to 'high'
        Rapid.collection(named: "my-todo-list")
            .filter(by: RapidFilter.equal(keyPath: "priority", value: "high"))
            .subscribe { result in
                // this will be called once and then every time a document is
                // added, updated or removed from a subset
                switch result {
                case .success(let toDos):
                    print(toDos)
                    
                case .failure(let error):
                    // TODO: Handle error
                    print(error)
                }
        }
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }


}

