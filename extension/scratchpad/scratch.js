/* Unused stuff*/

//Map a key to a set of values
class MultiMap{
    data = {};
    
    merge = (rhs) => {
        Object.keys(rhs.data).forEach( (k)=>{
            var obj = this.data[k];
            if(obj === undefined)
                this.data[k] = obj = {};
            Object.assign( obj, rhs.data[k] );
        });        
    }
    
    add = (k,v) => {
        var set = data[k];
        if(set === undefined)
            data[k] = { v: null };
        else
            set[v]=null;
    }
    
    get = (k) => {
        return data[k];
    }
    
    require = (k) => {
        var result = data[k];
        if(result === undefined)
            throw new RangeError("MultiMap key out of range");
        else
            return result;
    }
    
    remove_of_key = (k) => {
        data[k] = undefined;
    }
    
    remove = (k, v) => {
        var set = data[k];
        if(set === undefined)
            return;
        
        delete set[v];
        if(Object.keys(set).length <= 0)
            delete data[k];
    }
    
    forEach = (f) => {
        Object.keys(data).forEach( (set) => {
            Object.keys(set).forEach( (k)=>{
                f(k);
            });
        });
    }    
}