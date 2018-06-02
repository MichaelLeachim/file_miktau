package main

func main() {
	demo_data, _ := fs_backend.BuildAppStateOnAFolder("/home/mik/some.demo.project/")
	CNIS.MutableCreate(demo_data)

	// add dacha dataset
	reso := []*CoreNodeItem{}
	for i := 0; i <= 1000; i++ {
		reso = append(reso, buildDachaDataset()...)
	}
	CNIS.MutableCreate(reso)

	serve.Serve(4000)
}
